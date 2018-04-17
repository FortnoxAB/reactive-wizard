package se.fortnox.reactivewizard.db;

import com.google.common.collect.Lists;
import org.fest.assertions.Fail;
import org.junit.Test;
import rx.Observable;

import java.sql.Array;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ParameterizedQueryTest {

    MockDb  db      = new MockDb();
    DbProxy dbProxy = new DbProxy(db.getConnectionProvider());
    TestDao dao     = dbProxy.create(TestDao.class);

    @Test
    public void shouldResolveParametersFromQuery() throws SQLException {
        dao.namedParameters("myid", "myname").toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement("SELECT * FROM foo WHERE id=? AND name=?");
        verify(db.getPreparedStatement()).setObject(1, "myid");
        verify(db.getPreparedStatement()).setObject(2, "myname");
    }

    @Test
    public void shouldResolveNestedParametersFromQuery() throws SQLException {
        dao.nestedParameters("myid", new MyTestParam()).toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement("SELECT * FROM foo WHERE id=? AND name=?");
        verify(db.getPreparedStatement()).setObject(1, "myid");
        verify(db.getPreparedStatement()).setObject(2, "testName");
    }

    @Test
    public void shouldResolveParametersWithoutAnnotationFromQuery() throws SQLException {
        dao.missingParamNames("myid", "myname").toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement("SELECT * FROM foo WHERE id=? AND name=?");
        verify(db.getPreparedStatement()).setObject(1, "myid");
        verify(db.getPreparedStatement()).setObject(2, "myname");
    }

    @Test
    public void shouldThrowExceptionIfUnnamedParamsUsedInQuery() {
        try {
            dao.unnamedParameters("myid", "myname").toBlocking().singleOrDefault(null);
            Fail.fail("Exptected exception");
        } catch (Exception e) {
            assertThat(e.getMessage())
                .isEqualTo("Unnamed parameters are not supported: SELECT * FROM foo WHERE id=? AND name=?");
        }
    }

    @Test
    public void shouldThrowExceptionIfNotAllParametersAreFound() {
        try {
            dao.missingParamName("myid", "myname").toBlocking().singleOrDefault(null);
            Fail.fail("Exptected exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(
                "Query contains placeholder \"name\" but method noes not have such argument");
        }
    }

    @Test
    public void shouldSendEnumTypesAsStrings() throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setMyEnum(TestEnum.T3);
        dao.enumParameter(myobj).toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement("INSERT INTO a VALUES (?)");
        verify(db.getPreparedStatement()).setObject(1, "T3");
    }

    @Test
    public void shouldSupportGettersForBooleanThatHasIsAsPrefix()
        throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setFinished(true);

        dao.booleanWithIsPrefixAsParameter(myobj).toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement("INSERT INTO a VALUES (?)");
        verify(db.getPreparedStatement()).setObject(1, true);
    }

    @Test
    public void shouldSendMapTypesAsStrings() throws SQLException {

        TestObject myobj = new TestObject();
        myobj.setFinished(false);
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("aKey", "aValue");
        myobj.setMap(aMap);

        dao.mapParam(myobj).toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement(
            "INSERT INTO a (a, b, c) VALUES (?::json, ?, \"a\")");
        verify(db.getPreparedStatement()).setObject(1, "{\"aKey\":\"aValue\"}");
        verify(db.getPreparedStatement()).setObject(2, false);
    }

    @Test
    public void shouldSendMapTypesAsStringsAsLastArg() throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setFinished(false);
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("aKey", "aValue");
        myobj.setMap(aMap);

        dao.mapParamLast(myobj).toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement(
            "INSERT INTO a (a, b, c) VALUES (?, \"a\", ?::json)");
        verify(db.getPreparedStatement()).setObject(1, false);
        verify(db.getPreparedStatement()).setObject(2, "{\"aKey\":\"aValue\"}");
    }

    @Test
    public void shouldSendMapTypesAsStringsAsMiddleArg() throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setFinished(false);
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("aKey", "aValue");
        myobj.setMap(aMap);

        dao.mapParamMiddle(myobj).toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement(
            "INSERT INTO a (a, b, c) VALUES ( ?, ?::json, \"a\")");
        verify(db.getPreparedStatement()).setObject(1, false);
        verify(db.getPreparedStatement()).setObject(2, "{\"aKey\":\"aValue\"}");

    }

    @Test
    public void shouldHandleNotInClauseWithLongs() throws SQLException {
        List<Long> param = Lists.newArrayList(1L, 2L);
        when(db.getConnection().createArrayOf(any(), any())).thenReturn(mock(Array.class));

        dao.notInClauseBigint(param).toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement("SELECT a FROM b WHERE c !=ALL(?)");
        verify(db.getConnection()).createArrayOf("bigint", new Object[]{1L, 2L});

        verify(db.getPreparedStatement()).setArray(eq(1), any());
    }

    @Test
    public void shouldHandleInClauseWithStrings() throws SQLException {
        List<String> param = Lists.newArrayList("A", "B");
        when(db.getConnection().createArrayOf(any(), any())).thenReturn(mock(Array.class));

        dao.inClauseVarchar(param).toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement("SELECT x FROM y WHERE z =ANY(?)");
        verify(db.getConnection()).createArrayOf("varchar", new Object[]{"A", "B"});

        verify(db.getPreparedStatement()).setArray(eq(1), any());
    }

    @Test
    public void shouldHandleInClauseWithoutSpaceInSQL() throws SQLException {
        List<String> param = Lists.newArrayList("A", "B");
        when(db.getConnection().createArrayOf(any(), any())).thenReturn(mock(Array.class));

        dao.inClauseVarcharNoSpace(param).toBlocking().singleOrDefault(null);

        verify(db.getConnection()).prepareStatement("SELECT x FROM y WHERE z =ANY(?)");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrowIfUnsupportedArrayType() throws SQLException {
        List<Boolean> param = Lists.newArrayList(true, false);

        when(db.getConnection().createArrayOf(any(), any())).thenReturn(mock(Array.class));

        dao.unsupportedArrayType(param).toBlocking().singleOrDefault(null);
    }

    enum TestEnum {
        T1, T2, T3
    }

    interface TestDao extends Dao {
        @Query("SELECT * FROM foo WHERE id=:id AND name=:name")
        Observable<String> namedParameters(@Named("id") String id, @Named("name") String name);

        @Query("SELECT * FROM foo WHERE id=:id AND name=:test.name")
        Observable<String> nestedParameters(@Named("id") String id, @Named("test") MyTestParam test);

        @Query("SELECT * FROM foo WHERE id=? AND name=?")
        Observable<String> unnamedParameters(String id, String name);

        @Query("SELECT * FROM foo WHERE id=:id AND name=:name")
        Observable<String> missingParamName(String id, String misspelledName);

        @Query("SELECT * FROM foo WHERE id=:id AND name=:name")
        Observable<String> missingParamNames(String id, String name);

        @Query("INSERT INTO a VALUES (:myobj.myEnum)")
        Observable<String> enumParameter(@Named("myobj") TestObject testObject);

        @Query("INSERT INTO a VALUES (:myobj.finished)")
        Observable<String> booleanWithIsPrefixAsParameter(@Named("myobj") TestObject testObject);

        @Query("INSERT INTO a (a, b, c) VALUES (:myobj.map::json, :myobj.finished, \"a\")")
        Observable<String> mapParam(@Named("myobj") TestObject testObject);

        @Query("INSERT INTO a (a, b, c) VALUES (:myobj.finished, \"a\", :myobj.map::json)")
        Observable<String> mapParamLast(@Named("myobj") TestObject testObject);

        @Query("INSERT INTO a (a, b, c) VALUES ( :myobj.finished, :myobj.map::json, \"a\")")
        Observable<String> mapParamMiddle(@Named("myobj") TestObject testObject);

        @Query("SELECT a FROM b WHERE c NOT IN (:param)")
        Observable<String> notInClauseBigint(@Named("param") List<Long> param);

        @Query("SELECT x FROM y WHERE z IN (:param)")
        Observable<String> inClauseVarchar(@Named("param") List<String> param);

        @Query("SELECT x FROM y WHERE z IN(:param)")
        Observable<String> inClauseVarcharNoSpace(@Named("param") List<String> param);

        @Query("SELECT x FROM y WHERE z IN (:param)")
        Observable<String> unsupportedArrayType(@Named("param") List<Boolean> param);
    }

    public class TestObject {
        TestEnum            myEnum;
        boolean             finished;
        Map<String, String> map;

        public TestEnum getMyEnum() {
            return myEnum;
        }

        public void setMyEnum(TestEnum myEnum) {
            this.myEnum = myEnum;
        }

        public boolean isFinished() {
            return finished;
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }

    }

    public class MyTestParam {
        public String getName() {
            return "testName";
        }
    }
}
