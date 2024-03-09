package se.fortnox.reactivewizard.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Scheduler;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.statement.Statement;
import se.fortnox.reactivewizard.metrics.Metrics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.function.UnaryOperator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveStatementFactoryTest {
    @Mock
    private DatabaseConfig databaseConfig;

    @Mock
    private Scheduler scheduler;

    @Mock
    private Scheduler.Worker worker;

    private Statement statement;

    private ReactiveStatementFactory statementFactory;

    @BeforeEach
    public void setUp() {
        when(scheduler.createWorker()).thenReturn(worker);
        when(worker.schedule(any())).then(invocationOnMock -> {
            invocationOnMock.getArgument(0, Runnable.class).run();
            return mock(Disposable.class);
        });
        statement = new Statement() {
            private MonoSink monoSink;
            private FluxSink fluxSink;

            @Override
            public void execute(Connection connection) {
                fluxSink.next("result");
            }

            @Override
            public void onCompleted() {
                fluxSink.complete();
            }

            @Override
            public void onError(Throwable throwable) {
                fluxSink.error(throwable);
            }

            @Override
            public PreparedStatement batch(Connection connection, PreparedStatement preparedStatement) {
                return null;
            }

            @Override
            public void batchExecuted(int count) {

            }

            @Override
            public boolean sameBatch(Statement statement) {
                return false;
            }

            @Override
            public void setFluxSink(FluxSink<?> fluxSink) {
                this.fluxSink = fluxSink;
            }

            @Override
            public void setMonoSink(MonoSink monoSink) {
                this.monoSink = monoSink;
            }

        };
        statementFactory = new ReactiveStatementFactory(databaseConfig, scheduler, () -> mock(Connection.class));
    }

    @Test
    void shouldReleaseSchedulerWorkers() {
        Flux<Object> stmt = statementFactory.createFlux(Metrics.get("test"), () -> statement, UnaryOperator.identity());
        stmt.blockFirst();
        verify(scheduler).createWorker();
        verify(worker).dispose();
    }

    private interface TestDao {
        @Query("SELECT * FROM foo")
        Flux<String> select();
    }
}
