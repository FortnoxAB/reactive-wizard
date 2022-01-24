package se.fortnox.reactivewizard.db;

import org.fest.assertions.Fail;
import org.junit.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.statement.DbStatementFactoryFactory;
import se.fortnox.reactivewizard.db.statement.MinimumAffectedRowsException;
import se.fortnox.reactivewizard.db.transactions.DaoTransactions;
import se.fortnox.reactivewizard.db.transactions.DaoTransactionsFlux;
import se.fortnox.reactivewizard.db.transactions.DaoTransactionsFluxImpl;
import se.fortnox.reactivewizard.db.transactions.DaoTransactionsImpl;
import se.fortnox.reactivewizard.db.transactions.StatementContext;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import se.fortnox.reactivewizard.util.ReactiveDecorator;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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
    private DaoTransactions    daoTransactions    = new DaoTransactionsImpl();
    private DaoTransactionsFlux daoTransactionsFlux    = new DaoTransactionsFluxImpl();

    @Test
    public void shouldHaveEmptyInjectAnnotatedConstructor() {
        try {
            assertThat(DaoTransactionsImpl.class.getConstructor().getAnnotation(Inject.class)).isNotNull();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void shouldRunTwoQueriesInOneTransaction() throws SQLException {
        Observable<String> find1 = dao.find();
        Observable<String> find2 = dao.find();

        Observable<Void> executeTransactionObs = daoTransactions.executeTransaction(find1, find2);
        db.verifyConnectionsUsed(0);
        verify(db.getConnection(), times(0)).prepareStatement(any());

        executeTransactionObs.toBlocking().subscribe();

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
    public void shouldSupportFlux() throws SQLException {
        Mono<Long> count = daoTransactionsFlux.executeTransaction(dao.fluxFind(), dao.fluxFind()).count();

        count.block();

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
    public void shouldSupportMoreThan256Flux() throws SQLException {

        List<Flux<String>> finds = new ArrayList<>();

        for (int i = 0; i < 500; i++) {
            finds.add(dao.fluxFind());
        }

        daoTransactionsFlux.executeTransaction(finds).count().block();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(1)).setAutoCommit(false);
        verify(db.getConnection(), times(1)).commit();
        verify(db.getConnection(), times(finds.size())).prepareStatement("select * from test");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection(), times(1)).close();
        verify(db.getPreparedStatement(), times(finds.size())).close();
        verify(db.getResultSet(), times(finds.size())).close();
    }

    @Test
    public void shouldRunOnTransactionCompletedCallback() throws SQLException {
        when(db.getPreparedStatement().executeBatch())
            .thenReturn(new int[]{1, 1});

        final boolean[] cbExecuted   = {false, false, false, false};
        Observable<Integer> daoObsWithCb = dao.updateSuccess();

        Optional<StatementContext> decoration = ReactiveDecorator.getDecoration(daoObsWithCb);
        assertThat(decoration).isPresent();

        decoration.get().onTransactionCompleted(() -> cbExecuted[0] = true);

        daoObsWithCb = ReactiveDecorator.keepDecoration(daoObsWithCb, obs->{
            return obs.doOnCompleted(() -> cbExecuted[1] = true)
                .doOnSubscribe(() -> cbExecuted[2] = true)
                .doOnTerminate(() -> cbExecuted[3] = true);
        });

        Observable<Integer> updateSuccess = dao.updateSuccess();
        daoTransactions.executeTransaction(updateSuccess, daoObsWithCb).toBlocking().subscribe();

        assertThat(cbExecuted[0]).isTrue();

        // The Observable is actually never subscribed on in a transaction, so those will remain false
        assertThat(cbExecuted[1]).isFalse();
        assertThat(cbExecuted[2]).isFalse();
        assertThat(cbExecuted[3]).isFalse();
    }

    @Test
    public void shouldRunOnTransactionCompletedCallbackForFlux() throws SQLException {
        when(db.getPreparedStatement().executeBatch())
            .thenReturn(new int[]{1, 1});

        Runnable runnable = mock(Runnable.class);
        Flux<Integer> daoObsWithCb = dao.updateSuccessFlux();
        Optional<StatementContext> decoration = ReactiveDecorator.getDecoration(daoObsWithCb);
        decoration.get().onTransactionCompleted(runnable);

        daoTransactionsFlux.executeTransaction(dao.updateSuccessFlux(), daoObsWithCb).count().block();

        verify(runnable).run();
    }

    @Test
    public void subscribingToDaoObservableWillResultInTwoCallsToQuery() throws SQLException {
        Observable<GeneratedKey<Long>> update1 = dao.updateSuccessResultSet();
        Observable<GeneratedKey<Long>> update2 = dao.updateSuccessResultSet();

        daoTransactions.executeTransaction(update1, update2).toBlocking().subscribe();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(2)).prepareStatement(any(), anyInt());

        // TODO: Is this expected? Or do we want this to be handled in some other way?
        update2.toBlocking().singleOrDefault(null);

        db.verifyConnectionsUsed(2);
        verify(db.getConnection(), times(1)).setAutoCommit(false);
        verify(db.getConnection(), times(1)).commit();
        verify(db.getConnection(), times(3)).prepareStatement("update foo set key=val", 1);
        verify(db.getConnection(), timeout(500).times(2)).setAutoCommit(true);
        verify(db.getConnection(), times(2)).close();
        verify(db.getPreparedStatement(), times(3)).close();

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
    public void shouldNotFailIfQueryIsSubscribedTwice() throws SQLException {
        db.setUpdatedRows(1);

        final Observable<Integer> update = dao.updateSuccess();
        daoTransactions.executeTransaction(update).toBlocking().subscribe();
        update.toBlocking().single();
        update.toBlocking().single();

        Connection conn = db.getConnection();
        verify(conn, never()).rollback();
        verify(conn, times(1)).commit();
        verify(conn, times(1)).setAutoCommit(false);
        verify(conn, times(3)).setAutoCommit(true);
        verify(conn, times(3)).close();
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

    @Test
    public void shouldBeAbleToUseRetryOnFluxTransaction() throws SQLException {

        when(db.getPreparedStatement().getUpdateCount())
            .thenReturn(0);

        final Flux<Integer> update = dao.updateFailFlux();

        try {
            daoTransactionsFlux.executeTransaction(update).retry(3).count().block();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Connection conn = db.getConnection();
        verify(conn, times(4)).rollback();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfObservableIsNotFromDao() {
        daoTransactions.executeTransaction(Observable.empty());
    }

    @Test
    public void shouldAllowEmptyAndNullButNotNullInIterable() {
        try {
            daoTransactions.executeTransaction(Collections.emptyList());
            daoTransactions.executeTransaction((Iterable<Observable<Object>>) null);
        } catch (Exception e) {
            Fail.fail("Unexpected exception when testing transactions with empty and nulls");
        }

        try {
            daoTransactions.executeTransaction((Observable<Object>) null);
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).startsWith("All parameters to createTransaction needs to be observables coming from a Dao-class");
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

    @Test
    public void shoulUseSpecifiedConnectionAndScheduler() throws SQLException {
        MockDb otherDb = new MockDb();
        ConnectionProvider otherConnectionProvider = otherDb.getConnectionProvider();

        TestScheduler otherScheduler = new TestScheduler();
        DbProxy otherDbProxy = new DbProxy(
            new DatabaseConfig(), otherScheduler,
            null, new DbStatementFactoryFactory(), new JsonSerializerFactory());
        otherDbProxy = otherDbProxy.usingConnectionProvider(otherConnectionProvider);

        when(otherDb.getPreparedStatement().executeBatch())
            .thenReturn(new int[]{1, 1});

        TestDao otherTestDao = otherDbProxy.create(TestDao.class);
        daoTransactions.executeTransaction(otherTestDao.updateSuccess(), otherTestDao.updateSuccess())
            .subscribeOn(otherScheduler).subscribe();
        otherScheduler.triggerActions();

        otherDb.verifyConnectionsUsed(1);
        db.verifyConnectionsUsed(0);

        // Execute transaction without custom db proxy
        when(db.getPreparedStatement().executeBatch())
            .thenReturn(new int[]{1, 1});

        daoTransactions.executeTransaction(dao.updateSuccess(), dao.updateSuccess())
            .toBlocking().subscribe();
        otherDb.verifyConnectionsUsed(1);
        db.verifyConnectionsUsed(1);
    }

    @Test
    public void shoulUseSecondConnectionAndSchedulerIfFirstObservableHasNoConnectionProvider() throws SQLException {
        MockDb secondDb = new MockDb();
        ConnectionProvider otherConnectionProvider = secondDb.getConnectionProvider();

        DbProxy dbProxyWithoutConnectionProvider = new DbProxy(
            new DatabaseConfig(), Schedulers.io(),
            null, new DbStatementFactoryFactory(), new JsonSerializerFactory());
        DbProxy dbProxyWithConnectionProvider = dbProxyWithoutConnectionProvider.usingConnectionProvider(otherConnectionProvider);

        when(secondDb.getPreparedStatement().executeBatch())
            .thenReturn(new int[]{1, 1});

        TestDao daoWithConnectionProvider = dbProxyWithConnectionProvider.create(TestDao.class);
        TestDao daoWithoutConnectionProvider = dbProxyWithoutConnectionProvider.create(TestDao.class);

        daoTransactions.executeTransaction(daoWithoutConnectionProvider.updateSuccess(), daoWithConnectionProvider.updateSuccess())
            .test()
            .awaitTerminalEvent()
            .assertNoErrors();

        secondDb.verifyConnectionsUsed(1);
        db.verifyConnectionsUsed(0);

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> daoTransactions.executeTransaction(daoWithoutConnectionProvider.updateSuccess()).toBlocking().subscribe())
            .withMessage("No DaoObservable with a valid connection provider was found");
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

        @Update("update foo set other_key=val")
        Flux<Integer> updateFailFlux();

        @Update(value = "update foo set key=val", minimumAffected = 0)
        Observable<GeneratedKey<Long>> updateSuccessResultSet();

        @Query("select * from test")
        Flux<String> fluxFind();

        @Update(value = "update foo set key=val", minimumAffected = 0)
        Flux<Integer> updateSuccessFlux();
    }

}
