package se.fortnox.reactivewizard.db;

import org.fest.assertions.Fail;
import org.junit.Test;
import org.mockito.InOrder;
import rx.Observable;
import rx.Observer;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.statement.MinimumAffectedRowsException;
import se.fortnox.reactivewizard.db.transactions.DaoObservable;
import se.fortnox.reactivewizard.db.transactions.DaoTransactions;
import se.fortnox.reactivewizard.db.transactions.DaoTransactionsImpl;
import se.fortnox.reactivewizard.db.transactions.TransactionAlreadyExecutedException;
import se.fortnox.reactivewizard.test.TestUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static se.fortnox.reactivewizard.test.TestUtil.assertNestedException;

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

        daoTransactions.createTransaction(find1, find2);

        find1.subscribe();

        db.verifyConnectionsUsed(0);
        verify(db.getConnection(), times(0)).prepareStatement(any());

        find2.toBlocking().singleOrDefault(null);

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
    public void shouldRunOnTransactionCompletedCallback() throws SQLException {
        when(db.getPreparedStatement().executeBatch())
            .thenReturn(new int[]{1, 1});

        final boolean[] cbExecuted   = {false};
        DaoObservable   daoObsWithCb = ((DaoObservable)dao.updateSuccess()).doOnTransactionCompleted(() -> cbExecuted[0] = true);
        // doOnTransactionCompleted will only be called when using createTransaction

        Observable<Integer> updateSuccess = dao.updateSuccess();
        daoTransactions.createTransaction(updateSuccess, daoObsWithCb);
        updateSuccess.subscribe();
        daoObsWithCb.toBlocking().single();

        assertThat(cbExecuted[0]).isTrue();
    }

    @Test
    public void shouldRunTwoQueriesInTransactionOrderAndNotInSubscribeOrder() throws SQLException {
        Observable<String> find1 = dao.find();
        Observable<String> find2 = dao.find2();

        daoTransactions.createTransaction(find1, find2);

        find2.subscribe();
        find1.toBlocking().singleOrDefault(null);

        Connection connection = db.getConnection();
        InOrder    inOrder    = inOrder(connection);

        inOrder.verify(connection).prepareStatement("select * from test");
        inOrder.verify(connection).prepareStatement("select * from test2");
    }

    @Test
    public void shouldFailAllQueriesIfOneFails() throws SQLException {
        db.addRows(1);
        Observable<String> find1 = dao.find();
        Observable<String> find2 = dao.find();

        when(db.getPreparedStatement().executeQuery())
            .thenReturn(db.getResultSet())
            .thenThrow(new SQLException("error"));

        daoTransactions.createTransaction(find1, find2);

        Observer<String> find1Observer = mock(Observer.class);

        find1.subscribe(find1Observer);

        db.verifyConnectionsUsed(0);
        verify(db.getConnection(), times(0)).prepareStatement(any());

        try {
            find2.toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).isEqualTo("error");
        }

        verify(find1Observer).onError(TestUtil.matches(e -> {
            System.out.println(e.getMessage());
            assertNestedException(e, SQLException.class)
                .hasMessage("error");
        }));
        verify(find1Observer, times(0)).onCompleted();
        verify(find1Observer, times(1)).onNext(any());

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(1)).setAutoCommit(false);
        verify(db.getConnection(), times(1)).rollback();
        verify(db.getConnection(), times(2)).prepareStatement("select * from test");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection(), times(1)).close();
        verify(db.getPreparedStatement(), times(2)).close();
        verify(db.getResultSet(), times(1)).close();

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
    public void shouldFailIfQueryIsSubscribedTwice() throws SQLException {
        db.setUpdatedRows(1);

        final Observable<Integer> update = dao.updateSuccess();
        daoTransactions.createTransaction(update);

        update.toBlocking().single();
        try {
            update.toBlocking().single();
            fail("expected exception");
        } catch (Exception e) {
            assertNestedException(e, TransactionAlreadyExecutedException.class)
                .hasMessage("Transaction already executed. You cannot subscribe multiple times to an Observable that is part of a transaction.");
        }

        Connection conn = db.getConnection();
        verify(conn, never()).rollback();
        verify(conn, times(1)).commit();
        verify(conn).close();
    }

    @Test
    public void shouldBeAbleToUseRetryOnTransaction() throws SQLException {

        when(db.getPreparedStatement().getUpdateCount())
            .thenReturn(0);

        final Observable<Integer> update = dao.updateFail();

        daoTransactions.createTransaction(update);
        daoTransactions.executeTransaction(update).retry(3).test().awaitTerminalEvent();

        Connection conn = db.getConnection();
        verify(conn, times(4)).rollback();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfObservableIsNotFromDao() {
        daoTransactions.createTransaction(Observable.empty());
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
    public void shouldFailIfTransactionIsModifiedAfterCreation() throws Exception {
        db.addRows(1);
        Observable<String> find1 = dao.find();

        List<Observable<String>> transaction = new ArrayList<>();
        transaction.add(find1);

        daoTransactions.createTransaction(transaction);

        transaction.add(dao.find2());

        try {
            find1.toBlocking().single();
            fail("expected exception");
        } catch (Exception e) {
            if (e.getCause() != null) {
                e = (Exception)e.getCause();
            }
            assertThat(e)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transaction cannot be modified after creation.");
        }

        Connection conn = db.getConnection();
        verify(conn, never()).setAutoCommit(false);
        verify(conn, never()).commit();
        verify(conn, never()).rollback();
        verify(conn, never()).close();
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
