package se.fortnox.reactivewizard.db;

import org.junit.Test;
import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;

import java.sql.SQLException;

import static org.mockito.Mockito.verify;

public class DbConnectionTest {

    @Test
    public void shouldOnlyRequireOneConnection() throws SQLException {
        MockDb mockDb = new MockDb();
        mockDb.addRowColumn(1, 1, "testdata", String.class, "value");

	    DbProxy dbProxy = new DbProxy(new DatabaseConfig(), mockDb.getConnectionProvider());
        TestDao daoMock = dbProxy.create(TestDao.class);
        daoMock.list().map(s -> daoMock.list()).blockLast();

        mockDb.verifyConnectionsUsed(1);
        verify(mockDb.getPreparedStatement()).close();
    }

    interface TestDao {
        @Query("SELECT * FROM table")
        Flux<String> list();
    }
}
