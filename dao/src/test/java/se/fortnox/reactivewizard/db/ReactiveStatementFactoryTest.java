package se.fortnox.reactivewizard.db;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Scheduler;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactory;
import se.fortnox.reactivewizard.db.statement.Statement;
import se.fortnox.reactivewizard.db.transactions.ConnectionScheduler;
import se.fortnox.reactivewizard.metrics.PublisherMetrics;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.platform.commons.util.ReflectionUtils.getRequiredMethod;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReactiveStatementFactoryTest {
    @Mock
    private PagingOutput pagingOutput;

    @Mock
    private DbStatementFactory dbStatementFactory;

    @Mock
    private DatabaseConfig databaseConfig;

    @Mock
    private Scheduler scheduler;

    @Mock
    private Scheduler.Worker worker;

    private ReactiveStatementFactory statementFactory;

    @Before
    public void setUp() {
        when(pagingOutput.apply(any(), any())).then(invocationOnMock -> invocationOnMock.getArgument(0,
            Flux.class));
        when(scheduler.createWorker()).thenReturn(worker);
        when(worker.schedule(any())).then(invocationOnMock -> {
            invocationOnMock.getArgument(0, Runnable.class).run();
            return mock(Disposable.class);
        });
        when(dbStatementFactory.create(any())).then(invocationOnMock -> new Statement() {
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

        });

        statementFactory = new ReactiveStatementFactory(dbStatementFactory, pagingOutput, PublisherMetrics.get("test"), databaseConfig, o -> o, getRequiredMethod(TestDao.class, "select"));
    }

    @Test
    public void shouldReleaseSchedulerWorkers() {
        Flux<Object> stmt = (Flux<Object>) statementFactory.create(new Object[0], new ConnectionScheduler(() -> mock(Connection.class), scheduler));
        stmt.blockFirst();
        verify(scheduler).createWorker();
        verify(worker).dispose();
    }

    private interface TestDao {
        @Query("SELECT * FROM foo")
        Flux<String> select();
    }
}
