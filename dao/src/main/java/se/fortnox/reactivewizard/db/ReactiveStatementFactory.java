package se.fortnox.reactivewizard.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactory;
import se.fortnox.reactivewizard.db.statement.Statement;
import se.fortnox.reactivewizard.db.transactions.ConnectionScheduler;
import se.fortnox.reactivewizard.db.transactions.StatementContext;
import se.fortnox.reactivewizard.metrics.PublisherMetrics;
import se.fortnox.reactivewizard.util.DebugUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import static java.lang.String.format;
import static reactor.core.publisher.Flux.error;
import static se.fortnox.reactivewizard.util.ReactiveDecorator.decorated;

public class ReactiveStatementFactory {

    private static final int RECORD_BUFFER_SIZE = 100000;
    private static final Logger LOG = LoggerFactory.getLogger("Dao");
    private final DbStatementFactory statementFactory;
    private final PagingOutput pagingOutput;
    private final Function<Flux, Object> resultConverter;
    private final PublisherMetrics publisherMetrics;

    private final DatabaseConfig config;

    public ReactiveStatementFactory(
            DbStatementFactory statementFactory,
            PagingOutput pagingOutput,
            PublisherMetrics publisherMetrics,
            DatabaseConfig config,
            Function<Flux, Object> resultConverter) {
        this.statementFactory = statementFactory;
        this.pagingOutput = pagingOutput;
        this.publisherMetrics = publisherMetrics;
        this.config = config;
        this.resultConverter = resultConverter;
    }

    private static void closeSilently(Connection connection) {
        try {
            connection.close();
        } catch (SQLException sqlException) {
            LOG.warn("Failed to close connection", sqlException);
        }
    }

    /**
     * Create observable statement.
     *
     * @param args                the arguments
     * @param connectionScheduler the scheduler
     * @return the observable statement
     */
    public Object create(Object[] args, ConnectionScheduler connectionScheduler) {
        StatementContext statementContext = new StatementContext(() -> statementFactory.create(args), connectionScheduler);
        Flux<Object> result = Flux.create(fluxSink -> {
            try {
                statementContext.getConnectionScheduler()
                        .schedule(fluxSink::error, connection -> {
                            Statement dbStatement = statementContext.getStatement();
                            dbStatement.setFluxSink(fluxSink);
                            executeStatement(dbStatement, connection);
                        });
            } catch (Exception e) {
                if (!fluxSink.isCancelled()) {
                    fluxSink.error(e);
                }
            }
        }, FluxSink.OverflowStrategy.ERROR);

        if (DebugUtil.IS_DEBUG || LOG.isDebugEnabled()) {
            Exception queryFailure = new RuntimeException("Query failed");
            result = result.onErrorResume(thrown -> {
                queryFailure.initCause(thrown);
                return error(queryFailure);
            });
        }

        result = pagingOutput.apply(result, args);
        result = Flux.from(publisherMetrics.measure(result, this::logSlowQuery));
        result = result.onBackpressureBuffer(RECORD_BUFFER_SIZE);

        return decorated(resultConverter.apply(result), statementContext);
    }

    private void logSlowQuery(long time) {
        if (time > config.getSlowQueryLogThreshold()) {
            LOG.warn(format("Slow query: %s\ntime: %d", statementFactory, time));
        }
    }

    private void executeStatement(Statement dbStatement, Connection connection) {
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
