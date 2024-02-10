package se.fortnox.reactivewizard.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactory;
import se.fortnox.reactivewizard.db.statement.Statement;
import se.fortnox.reactivewizard.db.transactions.ConnectionScheduler;
import se.fortnox.reactivewizard.db.transactions.StatementContext;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.DebugUtil;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import static java.lang.String.format;
import static se.fortnox.reactivewizard.util.ReactiveDecorator.decorated;

public class ReactiveStatementFactory {

    private static final int RECORD_BUFFER_SIZE = 100000;
    private static final Logger LOG = LoggerFactory.getLogger("Dao");
    private static final String QUERY_FAILED = "Query failed";
    private final DbStatementFactory statementFactory;
    private final PagingOutput pagingOutput;
    private final Method method;
    private final Metrics metrics;

    private final DatabaseConfig config;

    public ReactiveStatementFactory(
            DbStatementFactory statementFactory,
            PagingOutput pagingOutput,
            Metrics metrics,
            DatabaseConfig config,
            Method method) {
        this.statementFactory = statementFactory;
        this.pagingOutput = pagingOutput;
        this.metrics = metrics;
        this.config = config;
        this.method = method;
    }


    private static void closeSilently(Connection connection) {
        try {
            connection.close();
        } catch (SQLException sqlException) {
            LOG.warn("Failed to close connection", sqlException);
        }
    }

    private Flux<Object> getResultFlux(StatementContext statementContext) {
        return Flux.create(fluxSink -> {
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
    }

    private Mono<Object> getResultMono(StatementContext statementContext) {
        return Mono.create(monoSink -> monoSink.onRequest(unusedRequestedAmount -> {
            try {
                statementContext.getConnectionScheduler()
                    .schedule(monoSink::error, connection -> {
                        Statement dbStatement = statementContext.getStatement();
                        dbStatement.setMonoSink(monoSink);
                        executeStatement(dbStatement, connection);
                    });
            } catch (Exception e) {
                monoSink.error(e);
            }
        }));
    }

    /**
     * Create Flux statement.
     *
     * @param args                the arguments
     * @param connectionScheduler the scheduler
     * @return the Flux statement
     */
    public Object create(Object[] args, ConnectionScheduler connectionScheduler) {
        StatementContext statementContext = new StatementContext(() -> statementFactory.create(args), connectionScheduler);
        if (Mono.class.isAssignableFrom(method.getReturnType())) {
            Mono<Object> resultMono = getResultMono(statementContext);
            if (shouldAddDebugErrorHandling()) {
                Exception queryFailure = new RuntimeException(QUERY_FAILED);
                resultMono = resultMono.onErrorResume(thrown -> {
                    queryFailure.initCause(thrown);
                    return Mono.error(queryFailure);
                });
            }
            resultMono = Mono.from(metrics.measure(resultMono, this::logSlowQuery));
            return decorated(resultMono, statementContext);
        } else if (Flux.class.isAssignableFrom(method.getReturnType())) {
            Flux<Object> resultFlux = getResultFlux(statementContext);
            if (shouldAddDebugErrorHandling()) {
                Exception queryFailure = new RuntimeException(QUERY_FAILED);
                resultFlux = resultFlux.onErrorResume(thrown -> {
                    queryFailure.initCause(thrown);
                    return Flux.error(queryFailure);
                });
            }
            resultFlux = pagingOutput.apply(resultFlux, args);
            resultFlux = Flux.from(metrics.measure(resultFlux, this::logSlowQuery));
            resultFlux = resultFlux.onBackpressureBuffer(RECORD_BUFFER_SIZE);
            return decorated(resultFlux, statementContext);
        } else {
            throw new IllegalArgumentException(String.format("DAO method %s::%s must return a Flux or Mono. Found %s",
                method.getDeclaringClass().getName(),
                method.getName(),
                method.getReturnType().getName()));
        }
    }

    private void logSlowQuery(long time) {
        if (time > config.getSlowQueryLogThreshold()) {
            LOG.warn(format("Slow query: %s%ntime: %d", statementFactory, time));
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

    private static boolean shouldAddDebugErrorHandling() {
        return DebugUtil.IS_DEBUG || LOG.isDebugEnabled();
    }
}
