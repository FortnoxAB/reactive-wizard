package se.fortnox.reactivewizard.db;

import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.statement.Statement;
import se.fortnox.reactivewizard.db.transactions.ConnectionScheduler;
import se.fortnox.reactivewizard.db.transactions.StatementContext;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.DebugUtil;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.lang.String.format;
import static se.fortnox.reactivewizard.util.ReactiveDecorator.decorated;

public class ReactiveStatementFactory {

    private static final int RECORD_BUFFER_SIZE = 100000;

    private static final Logger LOG = LoggerFactory.getLogger("Dao");

    private static final String QUERY_FAILED = "Query failed";

    private final DatabaseConfig config;

    private final Scheduler scheduler;

    private final ConnectionScheduler connectionScheduler;

    @Inject
    public ReactiveStatementFactory(DatabaseConfig config, @Nullable ConnectionProvider connectionProvider) {
        this(config, threadPool(config.getPoolSize()), connectionProvider);
    }

    public ReactiveStatementFactory(DatabaseConfig config, Scheduler scheduler,
        ConnectionProvider connectionProvider) {
        this.config = config;
        this.scheduler = scheduler;
        this.connectionScheduler = new ConnectionScheduler(connectionProvider, scheduler);
    }

    private static void closeSilently(Connection connection) {
        try {
            connection.close();
        } catch (SQLException sqlException) {
            LOG.warn("Failed to close connection", sqlException);
        }
    }

    private <T> Flux<T> getResultFlux(StatementContext statementContext) {
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

    private <T> Mono<T> getResultMono(StatementContext statementContext) {
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

    public <T> Mono<T> createMono(
        Metrics metrics,
        Supplier<Statement> statementSupplier
    ) {
        var statementContext = new StatementContext(statementSupplier, connectionScheduler);
        Mono<T> resultMono = getResultMono(statementContext);
        if (shouldAddDebugErrorHandling()) {
            resultMono = resultMono.onErrorResume(thrown ->
                Mono.error(new RuntimeException(QUERY_FAILED, thrown))
            );
        }
        resultMono = Mono.from(measure(resultMono, metrics));
        return decorated(resultMono, statementContext);
    }

    public <T> Flux<T> createFlux(
        Metrics metrics,
        Supplier<Statement> statementSupplier,
        UnaryOperator<Flux<T>> fluxMapper
    ) {
        var statementContext = new StatementContext(statementSupplier, connectionScheduler);
        Flux<T> resultFlux = getResultFlux(statementContext);
        if (shouldAddDebugErrorHandling()) {
            resultFlux = resultFlux.onErrorResume(thrown ->
                Flux.error(new RuntimeException(QUERY_FAILED, thrown))
            );
        }
        if (fluxMapper != null) {
            resultFlux = fluxMapper.apply(resultFlux);
        }
        resultFlux = Flux.from(measure(resultFlux, metrics));
        resultFlux = resultFlux.onBackpressureBuffer(RECORD_BUFFER_SIZE);
        return decorated(resultFlux, statementContext);
    }

    public <T> Flux<T> createFlux(
        Metrics metrics,
        Supplier<Statement> statementSupplier
    ) {
        return createFlux(metrics, statementSupplier, null);
    }

    private <T> Publisher<T> measure(Publisher<T> publisher, Metrics metrics) {
        return metrics.measure(publisher, time -> {
            if (time > config.getSlowQueryLogThreshold()) {
                LOG.warn("Slow query: {} time: {}", metrics.getName(), time);
            }
        });
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

    private static Scheduler threadPool(int poolSize) {
        if (poolSize == -1) {
            return Schedulers.boundedElastic();
        }
        return Schedulers.newBoundedElastic(10, Integer.MAX_VALUE, "DbProxy");
    }

    public ReactiveStatementFactory usingConnectionProvider(ConnectionProvider connectionProvider) {
        return new ReactiveStatementFactory(config, scheduler, connectionProvider);
    }

    public ReactiveStatementFactory usingConnectionProvider(ConnectionProvider connectionProvider, DatabaseConfig databaseConfig) {
        return new ReactiveStatementFactory(databaseConfig, scheduler, connectionProvider);
    }

    public ReactiveStatementFactory usingConnectionProvider(ConnectionProvider newConnectionProvider, Scheduler newScheduler) {
        return new ReactiveStatementFactory(config, newScheduler, newConnectionProvider);
    }

    public DatabaseConfig getDatabaseConfig() {
        return config;
    }
}
