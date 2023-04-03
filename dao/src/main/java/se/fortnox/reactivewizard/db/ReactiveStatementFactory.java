package se.fortnox.reactivewizard.db;

import org.reactivestreams.Publisher;
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
import se.fortnox.reactivewizard.metrics.PublisherMetrics;
import se.fortnox.reactivewizard.util.DebugUtil;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import static java.lang.String.format;
import static se.fortnox.reactivewizard.util.ReactiveDecorator.decorated;

public class ReactiveStatementFactory {

    private static final int RECORD_BUFFER_SIZE = 100000;
    private static final Logger LOG = LoggerFactory.getLogger("Dao");
    private static final String QUERY_FAILED = "Query failed";
    private final DbStatementFactory statementFactory;
    private final PagingOutput pagingOutput;
    private final Function<Publisher, Object> resultConverter;
    private final PublisherMetrics publisherMetrics;

    private final DatabaseConfig config;
    private final boolean isReturnTypeMono;

    public ReactiveStatementFactory(
        DbStatementFactory statementFactory,
        PagingOutput pagingOutput,
        PublisherMetrics publisherMetrics,
        DatabaseConfig config,
        Function<Publisher, Object> resultConverter,
        Method method) {
        this.statementFactory = statementFactory;
        this.pagingOutput = pagingOutput;
        this.publisherMetrics = publisherMetrics;
        this.config = config;
        this.resultConverter = resultConverter;
        isReturnTypeMono = Mono.class.isAssignableFrom(method.getReturnType());
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
        return Mono.create(monoSink -> {
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
        });
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
        if (isReturnTypeMono) {
            Mono<Object> resultMono = getResultMono(statementContext);
            if (shouldAddDebugErrorHandling()) {
                Exception queryFailure = new RuntimeException(QUERY_FAILED);
                resultMono = resultMono.onErrorResume(thrown -> {
                    queryFailure.initCause(thrown);
                    return Mono.error(queryFailure);
                });
            }
            resultMono = Mono.from(publisherMetrics.measure(resultMono, this::logSlowQuery));
            return decorated(resultConverter.apply(resultMono), statementContext);
        } else {
            Flux<Object> resultFlux = getResultFlux(statementContext);
            if (shouldAddDebugErrorHandling()) {
                Exception queryFailure = new RuntimeException(QUERY_FAILED);
                resultFlux = resultFlux.onErrorResume(thrown -> {
                    queryFailure.initCause(thrown);
                    return Flux.error(queryFailure);
                });
            }
            resultFlux = pagingOutput.apply(resultFlux, args);
            resultFlux = Flux.from(publisherMetrics.measure(resultFlux, this::logSlowQuery));
            resultFlux = resultFlux.onBackpressureBuffer(RECORD_BUFFER_SIZE);
            return decorated(resultConverter.apply(resultFlux), statementContext);
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
