package se.fortnox.reactivewizard.db;

import org.fest.assertions.Fail;
import org.junit.Test;
import rx.Observable;

import java.sql.SQLException;

import static org.mockito.Mockito.verify;
import static se.fortnox.reactivewizard.test.TestUtil.assertException;

public class SchemaAnnotationTest {

    MockDb                  db      = new MockDb();
    DbProxy                 dbProxy = new DbProxy(db.getConnectionProvider());
    SchemaAnnotationTestDao dao     = dbProxy.create(SchemaAnnotationTestDao.class);

    @Test
    public void shouldReplaceSchemaWithParameter() throws SQLException {
        dao.list("1").toBlocking().singleOrDefault(null);
        verify(db.getConnection()).prepareStatement("SELECT * FROM \"1\".table");
    }

    @Test
    public void shouldWorkWithMultipleRegularParameters()
        throws SQLException, NoSuchMethodException, SecurityException {

        dao.find("1", 123, "foobar").toBlocking().singleOrDefault(null);
        verify(db.getConnection()).prepareStatement(
            "SELECT * FROM \"1\".table WHERE id = ? AND foo = ?");
        verify(db.getPreparedStatement()).setObject(1, 123);
        verify(db.getPreparedStatement()).setObject(2, "foobar");
    }

    @Test
    public void shouldThrowException() throws SQLException {
        try {
            dao.list(null).toBlocking().singleOrDefault(null);
            Fail.fail("Expected exception");
        } catch (Exception e) {
            assertException(e, NullPointerException.class).hasMessage("Schema must not be null: systemId");
        }

    }

    interface SchemaAnnotationTestDao extends Dao {
        @Query("SELECT * FROM :systemId.table")
        Observable<String> list(@Schema("systemId") String systemId);

        @Query("SELECT * FROM :systemId.table WHERE id = :id AND foo = :foo")
        Observable<String> find(@Schema("systemId") String systemId,
            @Named("id") Integer id, @Named("foo") String foo
        );
    }
}
