package se.fortnox.reactivewizard.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactory;
import se.fortnox.reactivewizard.db.statement.Statement;
import se.fortnox.reactivewizard.db.transactions.DaoObservable;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.DebugUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static rx.Observable.error;

public class ObservableStatementFactory {

    private static final  int    RECORD_BUFFER_SIZE     = 100000;
    private static final Logger LOG                    = LoggerFactory.getLogger("Dao");
    private final DbStatementFactory         statementFactory;
    private final PagingOutput               pagingOutput;
    private final Scheduler                  scheduler;
    private final Metrics                    metrics;
    private final DatabaseConfig             config;

    public ObservableStatementFactory(
        DbStatementFactory statementFactory,
        PagingOutput pagingOutput,
        Scheduler scheduler,
        Function<Object[], String> paramSerializer,
        Metrics metrics,
        DatabaseConfig config
    ) {
        this.statementFactory = statementFactory;
        this.pagingOutput = pagingOutput;
        this.scheduler = scheduler;
        this.metrics = metrics;
        this.config = config;
    }

    private static void closeSilently(Connection connection) {
        try {
            connection.close();
        } catch (SQLException sqlException) {
            LOG.warn("Failed to close connection", sqlException);
        }
    }

    public Observable<Object> create(Object[] args, ConnectionProvider connectionProvider) {
        Supplier<Statement> statementSupplier = () -> statementFactory.create(args);
        Observable<Object> result = Observable.unsafeCreate(subscription -> {
            try {
                Statement dbStatement = statementSupplier.get();
                dbStatement.setSubscriber(subscription);

                scheduleWorker(subscription, () -> executeStatement(dbStatement, connectionProvider));
            } catch (Exception e) {
                if (!subscription.isUnsubscribed()) {
                    subscription.onError(e);
                }
            }
        });

        if (DebugUtil.IS_DEBUG || LOG.isDebugEnabled()) {
            Exception queryFailure = new RuntimeException("Query failed");
            result = result.onErrorResumeNext(thrown -> {
                queryFailure.initCause(thrown);
                return error(queryFailure);
            });
        }

        result = pagingOutput.apply(result, args);
        result = metrics.measure(result, this::logSlowQuery);
        result = result.onBackpressureBuffer(RECORD_BUFFER_SIZE);

        return new DaoObservable<>(result, statementSupplier);
    }

    private void scheduleWorker(Subscriber<?> subscription, Action0 action) {
        Scheduler.Worker worker = scheduler.createWorker();
        worker.schedule(() -> {
            try {
                action.call();
            } catch (Exception e) {
                if (!subscription.isUnsubscribed()) {
                    subscription.onError(e);
                }
            } finally {
                worker.unsubscribe();
            }
        });
    }

    private void logSlowQuery(long time) {
        if (time > config.getSlowQueryLogThreshold()) {
            LOG.warn(format("Slow query: %s\ntime: %d", statementFactory, time));
        }
    }

    private void executeStatement(Statement dbStatement, ConnectionProvider connectionProvider) {
        if (connectionProvider == null) {
            throw new RuntimeException("No connection provider configured!");
        }
        Connection connection = connectionProvider.get();
        try {
            connection.setAutoCommit(true);
            dbStatement.execute(connection);
            closeSilently(connection);
            dbStatement.onCompleted();
        } catch (Throwable e) {
            closeSilently(connection);
            dbStatement.onError(e);
        }
    }
}
