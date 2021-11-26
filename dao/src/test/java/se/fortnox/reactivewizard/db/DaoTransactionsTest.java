package se.fortnox.reactivewizard.db;

import org.fest.assertions.Fail;
import org.junit.Test;
import org.mockito.InOrder;
import rx.Observable;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.statement.MinimumAffectedRowsException;
import se.fortnox.reactivewizard.db.transactions.DaoTransactions;
import se.fortnox.reactivewizard.db.transactions.DaoTransactionsImpl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class DaoTransactionsTest {
    private MockDb             db                 = new MockDb();
    private ConnectionProvider connectionProvider = db.getConnectionProvider();
    private DbProxy            dbProxy            = new DbProxy(new DatabaseConfig(), connectionProvider);
    private TestDao            dao                = dbProxy.create(TestDao.class);
    private DaoTransactions    daoTransactions    = new DaoTransactionsImpl(connectionProvider, dbProxy);

    @Test
    public void shouldRunTwoQueriesInOneTransaction() throws SQLException {
        Observable<String> find1 = dao.find();
        Observable<String> find2 = dao.find();

        daoTransactions.executeTransaction(find1, find2).toBlocking().subscribe();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(1)).setAutoCommit(false);
        verify(db.getConnection(), times(1)).commit();
        verify(db.getConnection(), times(2)).prepareStatement("select * from test");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection(), times(1)).close();
        verify(db.getPreparedStatement(), times(2)).close();
        verify(db.getResultSet(), times(2)).close();
    }

    @Test
    public void subscribingToDaoObservableWillResultInTwoCallsToQuery() throws SQLException {
        Observable<String> find1 = dao.find();
        Observable<String> find2 = dao.find();

        daoTransactions.executeTransaction(find1, find2).toBlocking().subscribe();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(2)).prepareStatement(any());

        // TODO: Is this expected? Or do we want this to be handled in some other way?
        find2.toBlocking().singleOrDefault(null);

        db.verifyConnectionsUsed(2);
        verify(db.getConnection(), times(1)).setAutoCommit(false);
        verify(db.getConnection(), times(1)).commit();
        verify(db.getConnection(), times(3)).prepareStatement("select * from test");
        verify(db.getConnection(), timeout(500).times(2)).setAutoCommit(true);
        verify(db.getConnection(), times(2)).close();
        verify(db.getPreparedStatement(), times(3)).close();

        // TODO: Is this expected in a transaction?
        verify(db.getResultSet(), times(3)).close();
    }

    @Test
    public void shouldRunOnCompletedOnceWhenTransactionFinished() throws SQLException {
        AtomicInteger completed = new AtomicInteger();
        daoTransactions.executeTransaction(dao.updateSuccess(), dao.updateOtherSuccess(), dao.updateSuccess(), dao.updateOtherSuccess())
            .doOnCompleted(completed::incrementAndGet)
            .toBlocking().subscribe();

        assertThat(completed.get()).isEqualTo(1);

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(2)).prepareStatement("update foo set key=val");
        verify(db.getConnection(), times(2)).prepareStatement("update foo set key=val2");
    }

    @Test
    public void shouldNotRunOnCompletedWhenTransactionFailed() throws SQLException {
        Observable<Integer> find1 = dao.updateSuccess();
        Observable<Integer> find2 = dao.updateFail();

        AtomicInteger completed = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        try {
            daoTransactions.executeTransaction(find1, find2)
                .doOnCompleted(() -> completed.incrementAndGet())
                .doOnError(throwable -> failed.incrementAndGet())
                .toBlocking().subscribe();
            fail("exception expected");
        } catch (Exception e) {}

        assertThat(completed.get()).isEqualTo(0);
        assertThat(failed.get()).isEqualTo(1);

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(1)).setAutoCommit(false);
        verify(db.getConnection(), times(1)).rollback();
        verify(db.getConnection()).prepareStatement("update foo set key=val");
        verify(db.getConnection()).prepareStatement("update foo set other_key=val");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection(), times(1)).close();
        verify(db.getPreparedStatement(), times(2)).close();
    }

    @Test
    public void shouldRunTwoQueriesInTransactionOrder() throws SQLException {
        Observable<String> find1 = dao.find();
        Observable<String> find2 = dao.find2();

        daoTransactions.executeTransaction(find1, find2).toBlocking().subscribe();

        Connection connection = db.getConnection();
        InOrder    inOrder    = inOrder(connection);

        inOrder.verify(connection).prepareStatement("select * from test");
        inOrder.verify(connection).prepareStatement("select * from test2");
    }

    @Test
    public void shouldFailAllQueriesIfOneUpdateHasNotReachedMinimumUpdates() throws SQLException {
        when(db.getPreparedStatement().getUpdateCount())
            .thenReturn(1)
            .thenReturn(0);

        try {
            daoTransactions.executeTransaction(
                dao.updateSuccess(),
                dao.updateFail()
            ).toBlocking().single();
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(MinimumAffectedRowsException.class);
            assertThat(e.getCause().getMessage()).contains("Minimum affected rows not reached");
            assertThat(e.getCause().getMessage()).contains("Minimum: 1 actual: 0");
        }

        Connection conn = db.getConnection();
        verify(conn).setAutoCommit(false);
        verify(conn).rollback();
        verify(conn, never()).commit();
        verify(conn).close();
        verify(db.getPreparedStatement(), times(2)).close();
    }

    @Test
    public void shouldBatchWhenSameQuery() throws SQLException {
        when(db.getPreparedStatement().executeBatch())
            .thenReturn(new int[]{1, 1});

        daoTransactions.executeTransaction(
            dao.updateSuccess(),
            dao.updateSuccess()
        ).toBlocking().singleOrDefault(null);

        verify(db.getPreparedStatement(), times(2)).addBatch();

        Connection conn = db.getConnection();
        verify(conn).setAutoCommit(false);
        verify(conn).commit();
        verify(conn).close();
    }

    @Test
    public void shouldBatchWhenSameQueryInSequence() throws SQLException {
        when(db.getPreparedStatement().executeBatch())
            .thenReturn(new int[]{1, 1});
        when(db.getPreparedStatement().getUpdateCount())
            .thenReturn(1);

        daoTransactions.executeTransaction(
            dao.updateSuccess(),
            dao.updateSuccess(),
            dao.updateOtherSuccess()
        ).toBlocking().singleOrDefault(null);

        verify(db.getPreparedStatement(), times(2)).addBatch();
        verify(db.getPreparedStatement()).executeBatch();
        verify(db.getPreparedStatement()).executeUpdate();

        Connection conn = db.getConnection();
        verify(conn).setAutoCommit(false);
        verify(conn).commit();
        verify(conn).close();
    }

    @Test
    public void shouldBeAbleToUseRetryOnTransaction() throws SQLException {

        when(db.getPreparedStatement().getUpdateCount())
            .thenReturn(0);

        final Observable<Integer> update = dao.updateFail();

        daoTransactions.executeTransaction(update).retry(3).test().awaitTerminalEvent();

        Connection conn = db.getConnection();
        verify(conn, times(4)).rollback();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfObservableIsNotFromDao() {
        daoTransactions.executeTransaction(Observable.empty());
    }

    @Test
    public void shouldAllowEmptyAndNull() {
        try {
            daoTransactions.executeTransaction(Collections.emptyList());
            daoTransactions.executeTransaction((Iterable<Observable<Object>>) null);
            daoTransactions.executeTransaction();
        } catch (Exception e) {
            Fail.fail("Unexpected exception when testing transactions with empty and nulls");
        }
    }

    @Test
    public void shouldAllowEmptyListOfObservables() {
        try {
            daoTransactions.executeTransaction(Collections.emptyList()).toBlocking().lastOrDefault(null);
        } catch (Exception e) {
            Fail.fail("Unexpected exception when testing transactions with an empty list");
        }
    }

    @Test
    public void shouldAllowOnlyVoidsInTransaction() throws SQLException {
        when(db.getPreparedStatement().executeBatch())
            .thenReturn(new int[]{1, 1});
        try {
            daoTransactions.executeTransaction(asList(dao.updateSuccessVoid(),dao.updateSuccessVoid())).toBlocking().lastOrDefault(null);
        } catch (Exception e) {
            Fail.fail("Unexpected exception when testing transactions with only voids");
        }
    }

    @Test
    public void shouldIgnoreModificationToTransactionList() throws Exception {
        db.addRows(1);
        Observable<String> find1 = dao.find();

        List<Observable<String>> transaction = new ArrayList<>();
        transaction.add(find1);
        Observable<Void> transactionObservable = daoTransactions.executeTransaction(transaction);

        transaction.add(dao.find2());
        transactionObservable.toBlocking().subscribe();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(1)).setAutoCommit(false);
        verify(db.getConnection(), times(1)).commit();
        verify(db.getConnection(), times(1)).prepareStatement("select * from test");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection(), times(1)).close();
        verify(db.getPreparedStatement(), times(1)).close();
    }

    interface TestDao {
        @Query("select * from test")
        Observable<String> find();

        @Query("select * from test2")
        Observable<String> find2();

        @Update(value = "update foo set key=val", minimumAffected = 0)
        Observable<Integer> updateSuccess();

        @Update(value = "update foo set key=val", minimumAffected = 0)
        Observable<Void> updateSuccessVoid();

        @Update(value = "update foo set key=val2", minimumAffected = 0)
        Observable<Integer> updateOtherSuccess();

        @Update("update foo set other_key=val")
        Observable<Integer> updateFail();

        @Update(value = "update foo set key=val", minimumAffected = 0)
        Observable<GeneratedKey<Long>> updateSuccessResultSet();
    }

}
