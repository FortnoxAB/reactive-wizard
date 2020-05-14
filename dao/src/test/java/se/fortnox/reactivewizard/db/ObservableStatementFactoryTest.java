package se.fortnox.reactivewizard.db;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactory;
import se.fortnox.reactivewizard.db.statement.Statement;
import se.fortnox.reactivewizard.metrics.Metrics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ObservableStatementFactoryTest {
    @Mock
    private PagingOutput pagingOutput;

    @Mock
    private DbStatementFactory dbStatementFactory;

    @Mock
    private DatabaseConfig databaseConfig;

    @Mock
    private Scheduler scheduler;

    @Mock
    private rx.Scheduler.Worker worker;

    private MockDb mockDb;

    private ObservableStatementFactory statementFactory;

    @Before
    public void setUp() {
        mockDb = new MockDb();
        when(pagingOutput.apply(any(), anyVararg())).then(invocationOnMock -> invocationOnMock.getArgumentAt(0,
            Observable.class));
        when(scheduler.createWorker()).thenReturn(worker);
        when(worker.schedule(any())).then(invocationOnMock -> {
            invocationOnMock.getArgumentAt(0, Action0.class).call();
            return Subscriptions.unsubscribed();
        });
        when(dbStatementFactory.create(any(), any())).then(invocationOnMock -> new Statement() {
            @Override
            public void execute(Connection connection) {
                invocationOnMock.getArgumentAt(1, Subscriber.class).onNext("result");
            }

            @Override
            public void onCompleted() {
                invocationOnMock.getArgumentAt(1, Subscriber.class).onCompleted();
            }

            @Override
            public void onError(Throwable throwable) {
                invocationOnMock.getArgumentAt(1, Subscriber.class).onError(throwable);
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
        });
        Function<Object[], String> paramSerializer = objects -> "";
        statementFactory = new ObservableStatementFactory(dbStatementFactory, pagingOutput, scheduler, paramSerializer,
            Metrics.get("test"), databaseConfig);
    }

    @Test
    public void shouldReleaseSchedulerWorkers() {
        Observable<Object> stmt = statementFactory.create(new Object[0], mockDb.getConnectionProvider());
        stmt.toBlocking().single();
        verify(scheduler, times(1)).createWorker();
        verify(worker).unsubscribe();
    }
}
