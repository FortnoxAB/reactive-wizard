package se.fortnox.reactivewizard.db;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.transactions.DaoObservable;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DaoObservableTest {
    private MockDb                      db                 = new MockDb();
    private ConnectionProvider          connectionProvider = db.getConnectionProvider();
    private DbProxy                     dbProxy            = new DbProxy(new DatabaseConfig(), connectionProvider);
    private DaoTransactionsTest.TestDao dao                = dbProxy.create(DaoTransactionsTest.TestDao.class);

    @Test
    public void test() throws SQLException {
        AtomicInteger terminated = new AtomicInteger();
        AtomicInteger subscribed = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();

        Observable<String> find1 = ((DaoObservable<String>) dao.find())
            .onTerminate(terminated::incrementAndGet)
            .onSubscribe(subscribed::incrementAndGet)
            .doOnTransactionCompleted(completed::incrementAndGet)
            .onError(throwable -> error.incrementAndGet());

        Observable<Integer> updateFail = ((DaoObservable<Integer>) dao.updateFail())
            .onTerminate(terminated::incrementAndGet)
            .onSubscribe(subscribed::incrementAndGet)
            .doOnTransactionCompleted(completed::incrementAndGet)
            .onError(throwable -> error.incrementAndGet());

        find1.toBlocking().singleOrDefault(null);
        db.verifyConnectionsUsed(1);

        try {
            updateFail.toBlocking().singleOrDefault(null);
            Assertions.fail("expected exception");
        } catch (Exception e) {}

        db.verifyConnectionsUsed(2);
        verify(db.getConnection(), times(2)).setAutoCommit(true);
        verify(db.getConnection(), never()).commit();
        verify(db.getConnection(), times(1)).prepareStatement("select * from test");
        verify(db.getConnection(), times(1)).prepareStatement("update foo set other_key=val");
        verify(db.getConnection(), times(2)).close();
        verify(db.getPreparedStatement(), times(2)).close();
        verify(db.getResultSet(), times(1)).close();

        assertThat(subscribed.get()).isEqualTo(2);
        assertThat(terminated.get()).isEqualTo(2);
        assertThat(error.get()).isEqualTo(1);
        assertThat(completed.get()).isEqualTo(1);
    }
}
