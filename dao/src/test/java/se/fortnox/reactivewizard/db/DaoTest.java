package se.fortnox.reactivewizard.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.statement.DbStatementFactoryFactory;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DaoTest {
    private MockDb                      db;
    private DaoTransactionsTest.TestDao dao;

    /**
     * Resets the test state
     */
    @BeforeEach
    public void reset() {
        db  = new MockDb();
        dao = new DbProxy(
            new DatabaseConfig(),
            Schedulers.newBoundedElastic(1, Integer.MAX_VALUE, "DaoTestDbProxy"),
            db.getConnectionProvider(),
            new DbStatementFactoryFactory(),
            new JsonSerializerFactory()).create(DaoTransactionsTest.TestDao.class);
    }

    @Test
    void shouldUseNewConnectionOnMultipleDaoCalls() {
        StepVerifier.create(dao.find()).verifyComplete();
        db.verifyConnectionsUsed(1);
        StepVerifier.create(dao.updateFail()).verifyError();
        db.verifyConnectionsUsed(2);
    }

    @Test
    void shouldCompleteOnQueryAndUpdate() throws SQLException {
        StepVerifier.create(dao.find()).verifyComplete();
        StepVerifier.create(dao.updateFail()).verifyError();

        db.verifyConnectionsUsed(2);
        verify(db.getConnection(), times(2)).setAutoCommit(true);
        verify(db.getConnection(), never()).commit();
        verify(db.getConnection()).prepareStatement("select * from test");
        verify(db.getConnection()).prepareStatement("update foo set other_key=val");
        verify(db.getConnection(), times(2)).close();
        verify(db.getPreparedStatement(), times(2)).close();
        verify(db.getResultSet()).close();
    }

    @Test
    void subscriberShouldGetExactlyOneElementWhenRequestingOneElementFromConcatMap() throws SQLException, InterruptedException {
        db.addRows(1);

        AtomicInteger numNext = new AtomicInteger(0);
        AtomicReference<Throwable> error = new AtomicReference<>(null);

        Flux.just(1,2,3).concatMap(x -> dao.findMono().switchIfEmpty(Mono.error(RuntimeException::new)))
            .subscribe(
                next -> numNext.incrementAndGet(),
                error::set,
                () -> { },
                subscription -> subscription.request(1));

        Thread.sleep(3000);

        Throwable throwable = error.get();
        if (throwable != null) {
            fail("Expected one single next, got an error.", throwable);
        }

        assertThat(numNext.get()).isEqualTo(1);
    }
}
