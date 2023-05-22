package se.fortnox.reactivewizard.db;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParameterizedQueryTest {

    MockDb  db      = new MockDb();
    DbProxy dbProxy = new DbProxy(new DatabaseConfig(), db.getConnectionProvider());
    TestDao dao     = dbProxy.create(TestDao.class);

    @Test
    void shouldResolveParametersFromQuery() throws SQLException {
        dao.namedParameters("myid", "myname").blockLast();

        verify(db.getConnection()).prepareStatement("SELECT * FROM foo WHERE id=? AND name=?");
        verify(db.getPreparedStatement()).setObject(1, "myid");
        verify(db.getPreparedStatement()).setObject(2, "myname");
    }

    @Test
    void shouldResolveNestedParametersFromQuery() throws SQLException {
        dao.nestedParameters("myid", new MyTestParam()).blockLast();

        verify(db.getConnection()).prepareStatement("SELECT * FROM foo WHERE id=? AND name=?");
        verify(db.getPreparedStatement()).setObject(1, "myid");
        verify(db.getPreparedStatement()).setObject(2, "testName");
    }

    @Test
    void shouldResolveNestedRecordParametersFromQuery() throws SQLException {
        dao.nestedRecordParameters("myid", new MyTestParamRecord("testName")).blockLast();

        verify(db.getConnection()).prepareStatement("SELECT * FROM foo WHERE id=? AND name=?");
        verify(db.getPreparedStatement()).setObject(1, "myid");
        verify(db.getPreparedStatement()).setObject(2, "testName");
    }

    @Test
    void shouldResolveParametersWithoutAnnotationFromQuery() throws SQLException {
        dao.missingParamNames("myid", "myname").blockLast();

        verify(db.getConnection()).prepareStatement("SELECT * FROM foo WHERE id=? AND name=?");
        verify(db.getPreparedStatement()).setObject(1, "myid");
        verify(db.getPreparedStatement()).setObject(2, "myname");
    }

    @Test
    void shouldThrowExceptionIfUnnamedParamsUsedInQuery() {
        try {
            dao.unnamedParameters("myid", "myname").blockLast();
            fail("Exptected exception");
        } catch (Exception e) {
            assertThat(e.getMessage())
                .isEqualTo("Unnamed parameters are not supported: SELECT * FROM foo WHERE id=? AND name=?");
        }
    }

    @Test
    void shouldThrowExceptionIfNotAllParametersAreFound() {
        try {
            dao.missingParamName("myid", "myname").blockLast();
            fail("Exptected exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(
                "Query contains placeholder \"name\" but method noes not have such argument");
        }
    }

    @Test
    void shouldSendEnumTypesAsStrings() throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setMyEnum(TestEnum.T3);
        dao.enumParameter(myobj).block();

        verify(db.getConnection()).prepareStatement("INSERT INTO a VALUES (?)");
        verify(db.getPreparedStatement()).setObject(1, "T3");
    }

    @Test
    void shouldSupportGettersForBooleanThatHasIsAsPrefix()
        throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setFinished(true);

        dao.booleanWithIsPrefixAsParameter(myobj).block();

        verify(db.getConnection()).prepareStatement("INSERT INTO a VALUES (?)");
        verify(db.getPreparedStatement()).setObject(1, true);
    }

    @Test
    void shouldSendMapTypesAsStrings() throws SQLException {

        TestObject myobj = new TestObject();
        myobj.setFinished(false);
        Map<String, String> aMap = new HashMap<>();
        aMap.put("aKey", "aValue");
        myobj.setMap(aMap);

        dao.mapParam(myobj).block();

        verify(db.getConnection()).prepareStatement(
            "INSERT INTO a (a, b, c) VALUES (?::json, ?, \"a\")");
        verify(db.getPreparedStatement()).setObject(1, "{\"aKey\":\"aValue\"}");
        verify(db.getPreparedStatement()).setObject(2, false);
    }

    @Test
    void shouldSendMapTypesAsStringsAsLastArg() throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setFinished(false);
        Map<String, String> aMap = new HashMap<>();
        aMap.put("aKey", "aValue");
        myobj.setMap(aMap);

        dao.mapParamLast(myobj).block();

        verify(db.getConnection()).prepareStatement(
            "INSERT INTO a (a, b, c) VALUES (?, \"a\", ?::json)");
        verify(db.getPreparedStatement()).setObject(1, false);
        verify(db.getPreparedStatement()).setObject(2, "{\"aKey\":\"aValue\"}");
    }

    @Test
    void shouldSendMapTypesAsStringsAsMiddleArg() throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setFinished(false);
        Map<String, String> aMap = new HashMap<>();
        aMap.put("aKey", "aValue");
        myobj.setMap(aMap);

        dao.mapParamMiddle(myobj).block();

        verify(db.getConnection()).prepareStatement(
            "INSERT INTO a (a, b, c) VALUES ( ?, ?::json, \"a\")");
        verify(db.getPreparedStatement()).setObject(1, false);
        verify(db.getPreparedStatement()).setObject(2, "{\"aKey\":\"aValue\"}");

    }

    @Test
    void shouldSendListsOfObjectsAsJson() throws SQLException {
        TestObject myobj = new TestObject();
        List<MyTestParam> list = new ArrayList<>();
        list.add(new MyTestParam());
        list.add(new MyTestParam());
        myobj.setList(list);

        dao.listParam(myobj).block();

        verify(db.getConnection()).prepareStatement(
            "INSERT INTO a (a) VALUES (?)");
        verify(db.getPreparedStatement()).setObject(1, "[{\"name\":\"testName\"},{\"name\":\"testName\"}]");
    }

    @Test
    void shouldSendListsOfLongAsArray() throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setLongList(Lists.newArrayList(1L, 2L));

        dao.longListParam(myobj).block();

        verify(db.getConnection()).prepareStatement("INSERT INTO a (a) VALUES (?)");
        verify(db.getConnection()).createArrayOf("bigint", new Long[]{1L, 2L});

        verify(db.getPreparedStatement()).setArray(eq(1), any());
    }

    @Test
    void shouldSendListsOfIntegerAsArray() throws SQLException {
        TestObject myobj = new TestObject();
        myobj.setIntegerList(Lists.newArrayList(1, 2));

        dao.integerListParam(myobj).block();

        verify(db.getConnection()).prepareStatement("INSERT INTO a (a) VALUES (?)");
        verify(db.getConnection()).createArrayOf("integer", new Integer[]{1, 2});

        verify(db.getPreparedStatement()).setArray(eq(1), any());
    }

    @Test
    void shouldHandleNotInClauseWithLongs() throws SQLException {
        List<Long> param = Lists.newArrayList(1L, 2L);
        when(db.getConnection().createArrayOf(any(), any())).thenReturn(mock(Array.class));

        dao.notInClauseBigint(param).blockLast();

        verify(db.getConnection()).prepareStatement("SELECT a FROM b WHERE c !=ALL(?)");
        verify(db.getConnection()).createArrayOf("bigint", new Object[]{1L, 2L});

        verify(db.getPreparedStatement()).setArray(eq(1), any());
    }

    @Test
    void shouldHandleInClauseWithStrings() throws SQLException {
        List<String> param = Lists.newArrayList("A", "B");
        when(db.getConnection().createArrayOf(any(), any())).thenReturn(mock(Array.class));

        dao.inClauseVarchar(param).blockLast();

        verify(db.getConnection()).prepareStatement("SELECT x FROM y WHERE z =ANY(?)");
        verify(db.getConnection()).createArrayOf("varchar", new Object[]{"A", "B"});

        verify(db.getPreparedStatement()).setArray(eq(1), any());
    }

    @Test
    void shouldHandleInClauseWithoutSpaceInSQL() throws SQLException {
        List<String> param = Lists.newArrayList("A", "B");
        when(db.getConnection().createArrayOf(any(), any())).thenReturn(mock(Array.class));

        dao.inClauseVarcharNoSpace(param).blockLast();

        verify(db.getConnection()).prepareStatement("SELECT x FROM y WHERE z =ANY(?)");
    }

    @Test
    void shouldHandleLowerCaseInClauseInSQL() throws SQLException {
        List<String> param = Lists.newArrayList("A", "B");
        when(db.getConnection().createArrayOf(any(), any())).thenReturn(mock(Array.class));

        dao.lowerCaseInClauseVarchar(param).blockLast();

        verify(db.getConnection()).prepareStatement("SELECT x FROM y WHERE z =ANY(?)");
    }

    @Test
    void shouldHandleInClauseWithUUIDs() throws SQLException {
        // Given
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<UUID> param = Lists.newArrayList(uuid1, uuid2);
        when(db.getConnection().createArrayOf(any(), any())).thenReturn(mock(Array.class));

        // When
        dao.inClauseUuid(param).blockLast();

        // Then
        verify(db.getConnection()).prepareStatement("SELECT x FROM y WHERE z =ANY(?)");
        verify(db.getConnection()).createArrayOf("uuid", new Object[]{uuid1, uuid2});
    }

    enum TestEnum {
        T1, T2, T3
    }

    interface TestDao {
        @Query("SELECT * FROM foo WHERE id=:id AND name=:name")
        Flux<String> namedParameters(String id, String name);

        @Query("SELECT * FROM foo WHERE id=:id AND name=:test.name")
        Flux<String> nestedParameters(String id, MyTestParam test);

        @Query("SELECT * FROM foo WHERE id=:id AND name=:test.name")
        Flux<String> nestedRecordParameters(String id, MyTestParamRecord test);

        @Query("SELECT * FROM foo WHERE id=? AND name=?")
        Flux<String> unnamedParameters(String id, String name);

        @Query("SELECT * FROM foo WHERE id=:id AND name=:name")
        Flux<String> missingParamName(String id, String misspelledName);

        @Query("SELECT * FROM foo WHERE id=:id AND name=:name")
        Flux<String> missingParamNames(String id, String name);

        @Query("INSERT INTO a VALUES (:testObject.myEnum)")
        Mono<String> enumParameter(TestObject testObject);

        @Query("INSERT INTO a VALUES (:testObject.finished)")
        Mono<String> booleanWithIsPrefixAsParameter(TestObject testObject);

        @Query("INSERT INTO a (a, b, c) VALUES (:testObject.map::json, :testObject.finished, \"a\")")
        Mono<String> mapParam(TestObject testObject);

        @Query("INSERT INTO a (a, b, c) VALUES (:testObject.finished, \"a\", :testObject.map::json)")
        Mono<String> mapParamLast(TestObject testObject);

        @Query("INSERT INTO a (a, b, c) VALUES ( :testObject.finished, :testObject.map::json, \"a\")")
        Mono<String> mapParamMiddle(TestObject testObject);

        @Query("INSERT INTO a (a) VALUES (:testObject.list)")
        Mono<String> listParam(TestObject testObject);

        @Query("INSERT INTO a (a) VALUES (:testObject.longList)")
        Mono<String> longListParam(TestObject testObject);

        @Query("INSERT INTO a (a) VALUES (:testObject.integerList)")
        Mono<String> integerListParam(TestObject testObject);

        @Query("SELECT a FROM b WHERE c NOT IN (:param)")
        Flux<String> notInClauseBigint(List<Long> param);

        @Query("SELECT x FROM y WHERE z IN (:param)")
        Flux<String> inClauseVarchar(List<String> param);

        @Query("SELECT x FROM y WHERE z in (:param)")
        Flux<String> lowerCaseInClauseVarchar(List<String> param);

        @Query("SELECT x FROM y WHERE z IN(:param)")
        Flux<String> inClauseVarcharNoSpace(List<String> param);

        @Query("SELECT x FROM y WHERE z IN (:param)")
        Flux<String> inClauseUuid(List<UUID> param);

        @Query("SELECT x FROM y WHERE z IN (:param)")
        Flux<String> unsupportedArrayType(List<Boolean> param);
    }

    public class TestObject {
        TestEnum            myEnum;
        boolean             finished;
        Map<String, String> map;
        List<MyTestParam>   list;
        List<Long>          longList;
        List<Integer>       integerList;

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

        public List<MyTestParam> getList() {
            return list;
        }

        public void setList(List<MyTestParam> list) {
            this.list = list;
        }

        public List<Long> getLongList() {
            return longList;
        }

        public void setLongList(List<Long> longList) {
            this.longList = longList;
        }

        public List<Integer> getIntegerList() {
            return integerList;
        }

        public void setIntegerList(List<Integer> integerList) {
            this.integerList = integerList;
        }
    }

    public static class MyTestParam {
        public String getName() {
            return "testName";
        }
    }

    public record MyTestParamRecord(String name) {
    }
}
