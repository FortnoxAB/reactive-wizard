package se.fortnox.reactivewizard.db;

import org.junit.Test;
import rx.Observable;

import java.sql.SQLException;

import static org.mockito.Mockito.verify;

public class DbConnectionTest {

    @Test
    public void shouldOnlyRequireOneConnection() throws SQLException {
        MockDb mockDb = new MockDb();
        mockDb.addRowColumn(1, 1, "testdata", String.class, "value");

        DbProxy dbProxy = new DbProxy(mockDb.getConnectionProvider());
        TestDao daoMock = dbProxy.create(TestDao.class);
        daoMock.list("foo").map(s -> daoMock.list("bar")).toBlocking().single();

        mockDb.verifyConnectionsUsed(1);
        verify(mockDb.getPreparedStatement()).close();
    }

    interface TestDao extends Dao {
        @Query("SELECT * FROM :systemId.table")
        Observable<String> list(@Schema("systemId") String systemId);
    }
}
