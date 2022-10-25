package se.fortnox.reactivewizard.db;

import org.assertj.core.api.Fail;
import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.statement.DbStatementFactoryFactory;
import se.fortnox.reactivewizard.db.statement.MinimumAffectedRowsException;
import se.fortnox.reactivewizard.db.transactions.DaoTransactions;
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
    private MockDb db = new MockDb();
    private ConnectionProvider connectionProvider = db.getConnectionProvider();
    private DbProxy dbProxy = new DbProxy(new DatabaseConfig(), connectionProvider);
    private TestDao dao = dbProxy.create(TestDao.class);
    private DaoTransactions daoTransactions = new DaoTransactionsImpl();

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
        Flux<String> find1 = dao.find();
        Flux<String> find2 = dao.find();

        Mono<Void> executeTransactionObs = daoTransactions.executeTransaction(find1, find2);
        db.verifyConnectionsUsed(0);
        verify(db.getConnection(), times(0)).prepareStatement(any());

        executeTransactionObs.block();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection()).setAutoCommit(false);
        verify(db.getConnection()).commit();
        verify(db.getConnection(), times(2)).prepareStatement("select * from test");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection()).close();
        verify(db.getPreparedStatement(), times(2)).close();
        verify(db.getResultSet(), times(2)).close();
    }

    @Test
    public void shouldSupportFlux() throws SQLException {
        daoTransactions.executeTransaction(dao.find(), dao.find()).block();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection()).setAutoCommit(false);
        verify(db.getConnection()).commit();
        verify(db.getConnection(), times(2)).prepareStatement("select * from test");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection()).close();
        verify(db.getPreparedStatement(), times(2)).close();
        verify(db.getResultSet(), times(2)).close();
    }

    @Test
    public void shouldSupportMoreThan256Flux() throws SQLException {

        List<Publisher<String>> finds = new ArrayList<>();

        for (int i = 0; i < 500; i++) {
            finds.add(dao.find());
        }

        daoTransactions.executeTransaction(finds).block();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection()).setAutoCommit(false);
        verify(db.getConnection()).commit();
        verify(db.getConnection(), times(finds.size())).prepareStatement("select * from test");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection()).close();
        verify(db.getPreparedStatement(), times(finds.size())).close();
        verify(db.getResultSet(), times(finds.size())).close();
    }

    @Test
    public void shouldRunOnTransactionCompletedCallback() throws SQLException {
        when(db.getPreparedStatement().executeBatch())
                .thenReturn(new int[]{1, 1});

        final boolean[] cbExecuted = {false, false, false, false};
        Mono<Integer> daoMonoWithCb = dao.updateSuccess();

        Optional<StatementContext> decoration = ReactiveDecorator.getDecoration(daoMonoWithCb);
        assertThat(decoration).isPresent();

        decoration.get().onTransactionCompleted(() -> cbExecuted[0] = true);

        daoMonoWithCb = ReactiveDecorator.keepDecoration(daoMonoWithCb, obs -> obs.doOnSuccess((value) -> cbExecuted[1] = true)
                .doOnSubscribe((s) -> cbExecuted[2] = true)
                .doOnTerminate(() -> cbExecuted[3] = true));

        Mono<Integer> updateSuccess = dao.updateSuccess();
        daoTransactions.executeTransaction(updateSuccess, daoMonoWithCb).block();

        assertThat(cbExecuted[0]).isTrue();

        // The Mono is actually never subscribed on in a transaction, so those will remain false
        assertThat(cbExecuted[1]).isFalse();
        assertThat(cbExecuted[2]).isFalse();
        assertThat(cbExecuted[3]).isFalse();
    }

    @Test
    public void shouldRunOnTransactionCompletedFlux() throws SQLException {
        when(db.getPreparedStatement().executeBatch())
                .thenReturn(new int[]{1, 1});

        Runnable runnable = mock(Runnable.class);

        Flux<Integer> daoFluxWithCb = dao.updateSuccessFlux();

        Optional<StatementContext> fluxDecoration = ReactiveDecorator.getDecoration(daoFluxWithCb);
        fluxDecoration.get().onTransactionCompleted(runnable);

        daoTransactions.executeTransaction(dao.updateSuccessFlux(), daoFluxWithCb).block();

        verify(runnable).run();
    }

    @Test
    public void subscribingToDaoPublisherWillResultInTwoCallsToQuery() throws SQLException {
        Mono<GeneratedKey<Long>> update1 = dao.updateSuccessResultSet();
        Mono<GeneratedKey<Long>> update2 = dao.updateSuccessResultSet();

        daoTransactions.executeTransaction(update1, update2).block();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(2)).prepareStatement(any(), anyInt());

        // TODO: Is this expected? Or do we want this to be handled in some other way?
        update2.block();

        db.verifyConnectionsUsed(2);
        verify(db.getConnection()).setAutoCommit(false);
        verify(db.getConnection()).commit();
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
            .doOnSuccess((v) -> completed.incrementAndGet())
            .block();

        assertThat(completed.get()).isEqualTo(1);

        db.verifyConnectionsUsed(1);
        verify(db.getConnection(), times(2)).prepareStatement("update foo set key=val");
        verify(db.getConnection(), times(2)).prepareStatement("update foo set key=val2");
    }

    @Test
    public void shouldNotRunOnCompletedWhenTransactionFailed() throws SQLException {

        Mono<Integer> find1 = dao.updateSuccess();
        Flux<Integer> find2 = dao.updateFail();

        AtomicInteger completed = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        try {
            daoTransactions.executeTransaction(find1, find2)
                    .doOnSuccess((v) -> completed.incrementAndGet())
                    .doOnError(throwable -> failed.incrementAndGet())
                    .block();
            fail("exception expected");
        } catch (Exception e) {
        }

        assertThat(completed.get()).isZero();
        assertThat(failed.get()).isEqualTo(1);

        db.verifyConnectionsUsed(1);
        verify(db.getConnection()).setAutoCommit(false);
        verify(db.getConnection()).rollback();
        verify(db.getConnection()).prepareStatement("update foo set key=val");
        verify(db.getConnection()).prepareStatement("update foo set other_key=val");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection()).close();
        verify(db.getPreparedStatement(), times(2)).close();
    }

    @Test
    public void shouldRunTwoQueriesInTransactionOrder() throws SQLException {
        Flux<String> find1 = dao.find();
        Flux<String> find2 = dao.find2();

        daoTransactions.executeTransaction(find1, find2).block();

        Connection connection = db.getConnection();
        InOrder inOrder = inOrder(connection);

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
            ).block();
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
        ).block();

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
        ).block();

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

        final Mono<Integer> update = dao.updateSuccess();
        daoTransactions.executeTransaction(update).block();
        update.block();
        update.block();

        Connection conn = db.getConnection();
        verify(conn, never()).rollback();
        verify(conn).commit();
        verify(conn).setAutoCommit(false);
        verify(conn, times(3)).setAutoCommit(true);
        verify(conn, times(3)).close();
    }

    @Test
    public void shouldBeAbleToUseRetryOnTransaction() throws SQLException {

        when(db.getPreparedStatement().getUpdateCount())
                .thenReturn(0);

        final Flux<Integer> update = dao.updateFail();
        StepVerifier.create(daoTransactions.executeTransaction(update).retry(3)).verifyError();

        Connection conn = db.getConnection();
        verify(conn, times(4)).rollback();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfPublisherIsNotFromDao() {
        daoTransactions.executeTransaction(Flux.empty(), Mono.empty());
    }

    @Test
    public void shouldAllowEmptyAndNullButNotNullInIterable() {
        try {
            daoTransactions.executeTransaction(Collections.emptyList());
            daoTransactions.executeTransaction((Iterable<Publisher<Object>>) null);
        } catch (Exception e) {
            Fail.fail("Unexpected exception when testing transactions with empty and nulls");
        }

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> daoTransactions.executeTransaction((Publisher<Object>) null))
            .withMessageStartingWith("All parameters to createTransaction needs to be Publishers coming from a Dao-class, i.e. decorated. Statement was");
    }

    @Test
    public void shouldAllowEmptyListOfPublishers() {
        try {
            daoTransactions.executeTransaction(Collections.emptyList()).block();
        } catch (Exception e) {
            Fail.fail("Unexpected exception when testing transactions with an empty list");
        }
    }

    @Test
    public void shouldAllowOnlyVoidsInTransaction() throws SQLException {
        when(db.getPreparedStatement().executeBatch())
                .thenReturn(new int[]{1, 1});
        try {
            daoTransactions.executeTransaction(asList(dao.updateSuccessVoid(), dao.updateSuccessVoid())).block();
        } catch (Exception e) {
            Fail.fail("Unexpected exception when testing transactions with only voids");
        }
    }

    @Test
    public void shouldIgnoreModificationToTransactionList() throws Exception {
        db.addRows(1);
        Flux<String> find1 = dao.find();

        List<Flux<String>> transaction = new ArrayList<>();
        transaction.add(find1);
        Mono<Void> transactionMono = daoTransactions.executeTransaction(transaction.stream().map(fluxString -> (Publisher<String>)fluxString).toList());

        transaction.add(dao.find2());
        transactionMono.block();

        db.verifyConnectionsUsed(1);
        verify(db.getConnection()).setAutoCommit(false);
        verify(db.getConnection()).commit();
        verify(db.getConnection()).prepareStatement("select * from test");
        verify(db.getConnection(), timeout(500)).setAutoCommit(true);
        verify(db.getConnection()).close();
        verify(db.getPreparedStatement()).close();
    }

    @Test
    public void shouldUseSpecifiedConnectionAndScheduler() throws SQLException {
        MockDb otherDb = new MockDb();
        ConnectionProvider otherConnectionProvider = otherDb.getConnectionProvider();

        VirtualTimeScheduler otherScheduler = VirtualTimeScheduler.create();
        DbProxy otherDbProxy = new DbProxy(
            new DatabaseConfig(), otherScheduler,
            null, new DbStatementFactoryFactory(), new JsonSerializerFactory());
        otherDbProxy = otherDbProxy.usingConnectionProvider(otherConnectionProvider);

        when(otherDb.getPreparedStatement().executeBatch())
                .thenReturn(new int[]{1, 1});

        TestDao otherTestDao = otherDbProxy.create(TestDao.class);
        daoTransactions.executeTransaction(otherTestDao.updateSuccess(), otherTestDao.updateSuccess())
                .subscribeOn(otherScheduler).subscribe();
        otherScheduler.advanceTime();

        otherDb.verifyConnectionsUsed(1);
        db.verifyConnectionsUsed(0);

        // Execute transaction without custom db proxy
        when(db.getPreparedStatement().executeBatch())
                .thenReturn(new int[]{1, 1});

        daoTransactions.executeTransaction(dao.updateSuccess(), dao.updateSuccess()).block();
        otherDb.verifyConnectionsUsed(1);
        db.verifyConnectionsUsed(1);
    }

    @Test
    public void shouldUseSecondConnectionAndSchedulerIfFirstPublisherHasNoConnectionProvider() throws SQLException {
        MockDb secondDb = new MockDb();
        ConnectionProvider otherConnectionProvider = secondDb.getConnectionProvider();

        DbProxy dbProxyWithoutConnectionProvider = new DbProxy(
                new DatabaseConfig(), Schedulers.boundedElastic(), null, new DbStatementFactoryFactory(), new JsonSerializerFactory());
        DbProxy dbProxyWithConnectionProvider = dbProxyWithoutConnectionProvider.usingConnectionProvider(otherConnectionProvider);

        when(secondDb.getPreparedStatement().executeBatch())
                .thenReturn(new int[]{1, 1});

        TestDao daoWithConnectionProvider = dbProxyWithConnectionProvider.create(TestDao.class);
        TestDao daoWithoutConnectionProvider = dbProxyWithoutConnectionProvider.create(TestDao.class);
        StepVerifier.create(daoTransactions.executeTransaction(daoWithoutConnectionProvider.updateSuccess(), daoWithConnectionProvider.updateSuccess()))
            .verifyComplete();

        secondDb.verifyConnectionsUsed(1);
        db.verifyConnectionsUsed(0);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> daoTransactions.executeTransaction(daoWithoutConnectionProvider.updateSuccess()).block())
                .withMessage("No Publisher with a valid connection provider was found");
    }

    interface TestDao {
        @Query("select * from test")
        Flux<String> find();

        @Query("select * from test2")
        Flux<String> find2();

        @Update(value = "update foo set key=val", minimumAffected = 0)
        Mono<Integer> updateSuccess();

        @Update(value = "update foo set key=val", minimumAffected = 0)
        Flux<Integer> updateSuccessFlux();

        @Update(value = "update foo set key=val", minimumAffected = 0)
        Mono<Void> updateSuccessVoid();

        @Update(value = "update foo set key=val2", minimumAffected = 0)
        Mono<Integer> updateOtherSuccess();

        @Update("update foo set other_key=val")
        Flux<Integer> updateFail();

        @Update(value = "update foo set key=val", minimumAffected = 0)
        Mono<GeneratedKey<Long>> updateSuccessResultSet();
    }

}
