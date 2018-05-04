package se.fortnox.reactivewizard.db;

import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import org.junit.Test;
import rx.Observable;

import java.sql.SQLException;

import static org.mockito.Mockito.verify;

public class DbConnectionTest {

    @Test
    public void shouldOnlyRequireOneConnection() throws SQLException {
        MockDb mockDb = new MockDb();
        mockDb.addRowColumn(1, 1, "testdata", String.class, "value");

	    DbProxy dbProxy = new DbProxy(new DatabaseConfig(), mockDb.getConnectionProvider());
        TestDao daoMock = dbProxy.create(TestDao.class);
        daoMock.list().map(s -> daoMock.list()).toBlocking().single();

        mockDb.verifyConnectionsUsed(1);
        verify(mockDb.getPreparedStatement()).close();
    }

    interface TestDao {
        @Query("SELECT * FROM table")
        Observable<String> list();
    }
}
