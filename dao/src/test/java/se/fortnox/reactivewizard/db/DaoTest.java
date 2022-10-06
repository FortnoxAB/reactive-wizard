package se.fortnox.reactivewizard.db;

import org.junit.Before;
import org.junit.Test;
import reactor.test.StepVerifier;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;

import java.sql.SQLException;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DaoTest {
    private MockDb                      db;
    private DaoTransactionsTest.TestDao dao;

    /**
     * Resets the test state
     */
    @Before
    public void reset() {
        db  = new MockDb();
        dao = new DbProxy(new DatabaseConfig(), db.getConnectionProvider()).create(DaoTransactionsTest.TestDao.class);
    }

    @Test
    public void shouldUseNewConnectionOnMultipleDaoCalls() {
        StepVerifier.create(dao.find()).verifyComplete();
        db.verifyConnectionsUsed(1);
        StepVerifier.create(dao.updateFail()).verifyError();
        db.verifyConnectionsUsed(2);
    }

    @Test
    public void shouldCompleteOnQueryAndUpdate() throws SQLException {
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
}
