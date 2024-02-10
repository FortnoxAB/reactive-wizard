package se.fortnox.reactivewizard.db;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.fortnox.reactivewizard.db.deserializing.DbResultSetDeserializerImpl;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbResultSetDeserializerImplTest {

    private final ResultSet         rs        = mock(ResultSet.class);
    private final ResultSetMetaData meta      = mock(ResultSetMetaData.class);
    private final Array             jdbcArray = mock(Array.class);
    private       TimeZone          previousTimeZone;

    @BeforeEach
    public void setup() {
        previousTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+2:00"));
    }

    @AfterEach
    public void reset() {
        TimeZone.setDefault(previousTimeZone);
    }

    @Test
    void shouldDeserializeString() throws SQLException {
        when(rs.getString(1)).thenReturn("Test");
        thenDeserialized(String.class).isEqualTo("Test");
    }

    @Test
    void shouldDeserializeUUID() throws SQLException {
        UUID uuid = UUID.randomUUID();
        when(rs.getObject(1)).thenReturn(uuid);
        thenDeserialized(UUID.class).isEqualTo(uuid);
    }

    @Test
    void shouldDeserializeNullString() throws SQLException {
        when(rs.getString(1)).thenReturn(null);
        when(rs.wasNull()).thenReturn(true);
        thenDeserialized(String.class).isNull();
    }

    @Test
    void shouldDeserializeInt() throws SQLException {
        when(rs.getInt(1)).thenReturn(567);
        thenDeserialized(int.class).isEqualTo(567);
        thenDeserialized(Integer.class).isEqualTo(567);
    }

    @Test
    void shouldDeserializeDouble() throws SQLException {
        when(rs.getDouble(1)).thenReturn(567d);
        thenDeserialized(double.class).isEqualTo(567d);
        thenDeserialized(Double.class).isEqualTo(567d);
    }

    @Test
    void shouldDeserializeFloat() throws SQLException {
        when(rs.getFloat(1)).thenReturn(567f);
        thenDeserialized(float.class).isEqualTo(567f);
        thenDeserialized(Float.class).isEqualTo(567f);
    }

    @Test
    void shouldDeserializeNullInt() throws SQLException {
        when(rs.getInt(1)).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);
        thenDeserialized(int.class).isEqualTo(0);
        thenDeserialized(Integer.class).isNull();
    }

    @Test
    void shouldDeserializeLong() throws SQLException {
        when(rs.getLong(1)).thenReturn(567888L);
        thenDeserialized(long.class).isEqualTo(567888L);
        thenDeserialized(Long.class).isEqualTo(567888L);
    }

    @Test
    void shouldDeserializeNullLong() throws SQLException {
        when(rs.getLong(1)).thenReturn(0L);
        when(rs.wasNull()).thenReturn(true);
        thenDeserialized(long.class).isEqualTo(0L);
        thenDeserialized(Long.class).isNull();
    }

    @Test
    void shouldDeserializeBigDecimal() throws SQLException {
        when(rs.getBigDecimal(1)).thenReturn(BigDecimal.TEN);
        thenDeserialized(BigDecimal.class).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void shouldDeserializeNullBigDecimal() throws SQLException {
        when(rs.getBigDecimal(1)).thenReturn(null);
        thenDeserialized(BigDecimal.class).isNull();
    }

    @Test
    void shouldDeserializeBoolean() throws SQLException {
        when(rs.getBoolean(1)).thenReturn(true);
        thenDeserialized(Boolean.class).isEqualTo(Boolean.TRUE);
        thenDeserialized(boolean.class).isEqualTo(Boolean.TRUE);

        when(rs.getBoolean(1)).thenReturn(false);
        thenDeserialized(Boolean.class).isEqualTo(Boolean.FALSE);
    }

    @Test
    void shouldDeserializeNullBoolean() throws SQLException {
        when(rs.getBoolean(1)).thenReturn(false);
        when(rs.wasNull()).thenReturn(true);
        thenDeserialized(Boolean.class).isNull();
        thenDeserialized(boolean.class).isEqualTo(Boolean.FALSE);
    }

    @Test
    void shouldDeserializeLocalDate() throws SQLException {
        when(rs.getDate(1)).thenReturn(new java.sql.Date(1262300400000L));
        thenDeserialized(LocalDate.class).isEqualTo(LocalDate.parse("2010-01-01"));
        when(rs.getDate(1)).thenReturn(null);
        thenDeserialized(LocalDate.class).isNull();
    }

    @Test
    void shouldDeserializeLocalDateTime() throws SQLException {
        when(rs.getTimestamp(1)).thenReturn(new java.sql.Timestamp(1472739367000L));
        thenDeserialized(LocalDateTime.class).isEqualTo(LocalDateTime.parse("2016-09-01T16:16:07"));
        when(rs.getTimestamp(1)).thenReturn(null);
        thenDeserialized(LocalDateTime.class).isNull();
    }

    @Test
    void shouldDeserializeYearMonth() throws SQLException {
        when(rs.getInt(1)).thenReturn(202110);
        thenDeserialized(YearMonth.class).isEqualTo(YearMonth.parse("2021-10"));
        when(rs.getInt(1)).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);
        thenDeserialized(YearMonth.class).isNull();
    }

    @Test
    void shouldDeserializeEnum() throws SQLException {
        when(rs.getString(1)).thenReturn("T2");
        thenDeserialized(TestEnum.class).isEqualTo(TestEnum.T2);
    }

    @Test
    void shouldDeserializeLocalTime() throws SQLException {
        when(rs.getTime(1)).thenReturn(new java.sql.Time(1466163420000L));
        thenDeserialized(LocalTime.class).isEqualTo(LocalTime.parse("13:37:00"));
    }

    @Test
    void shouldDeserializeNullTime() throws SQLException {
        when(rs.getTime(1)).thenReturn(null);
        when(rs.wasNull()).thenReturn(true);
        thenDeserialized(LocalTime.class).isNull();
    }

    @Test
    void shouldDeserializeNullEnum() throws SQLException {
        when(rs.getString(1)).thenReturn(null);
        when(rs.wasNull()).thenReturn(true);
        thenDeserialized(TestEnum.class).isNull();
    }

    @Test
    void shouldDeserializeImmutableObject() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(ImmutableDbTestObj.class);
        when(meta.getColumnCount()).thenReturn(5);
        when(meta.getColumnLabel(1)).thenReturn("sql_val");
        when(meta.getColumnLabel(2)).thenReturn("my_bool");
        when(meta.getColumnLabel(3)).thenReturn("enum_val");
        when(meta.getColumnLabel(4)).thenReturn("child.sql_val");
        when(meta.getColumnLabel(5)).thenReturn("child.no_getter");
        when(rs.getString(1)).thenReturn("MyValue");
        when(rs.getBoolean(2)).thenReturn(true);
        when(rs.getString(3)).thenReturn("T3");
        when(rs.getString(4)).thenReturn("MyChildValue");
        when(rs.getString(5)).thenReturn("NoGetterValue");
        when(rs.getMetaData()).thenReturn(meta);
        ImmutableDbTestObj myTestObj = (ImmutableDbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getSqlVal()).isEqualTo("MyValue");
        assertThat(myTestObj.getMyBool()).isEqualTo(Boolean.TRUE);
        Assertions.assertThat(myTestObj.getEnumVal()).isEqualTo(TestEnum.T3);
        assertThat(myTestObj.getChild()).isNotNull();
        assertThat(myTestObj.getChild().getSqlVal()).isEqualTo("MyChildValue");
        assertThat(myTestObj.getChild().getValueWithoutGetter()).isEqualTo("NoGetterValue");
    }

    @Test
    void shouldDeserializeObject() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(3);
        when(meta.getColumnLabel(1)).thenReturn("sql_val");
        when(meta.getColumnLabel(2)).thenReturn("my_bool");
        when(meta.getColumnLabel(3)).thenReturn("enum_val");
        when(rs.getString(1)).thenReturn("MyValue");
        when(rs.getBoolean(2)).thenReturn(true);
        when(rs.getString(3)).thenReturn("T3");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getSqlVal()).isEqualTo("MyValue");
        assertThat(myTestObj.getMyBool()).isEqualTo(Boolean.TRUE);
        Assertions.assertThat(myTestObj.getEnumVal()).isEqualTo(TestEnum.T3);
    }

    @Test
    void shouldDeserializeObjectRecord() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        when(meta.getColumnCount()).thenReturn(3);
        when(meta.getColumnLabel(1)).thenReturn("sql_val");
        when(meta.getColumnLabel(2)).thenReturn("my_bool");
        when(meta.getColumnLabel(3)).thenReturn("enum_val");
        when(rs.getString(1)).thenReturn("MyValue");
        when(rs.getBoolean(2)).thenReturn(true);
        when(rs.getString(3)).thenReturn("T3");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.sqlVal()).isEqualTo("MyValue");
        assertThat(myTestObj.myBool()).isEqualTo(Boolean.TRUE);
        Assertions.assertThat(myTestObj.enumVal()).isEqualTo(TestEnum.T3);
    }

    @Test
    void shouldDeserializeChildObject() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("child.sql_val");
        when(rs.getString(1)).thenReturn("MyChildValue");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getChild().getSqlVal()).isEqualTo("MyChildValue");
    }

    @Test
    void shouldDeserializeChildObjectRecord() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("child.sql_val");
        when(rs.getString(1)).thenReturn("MyChildValue");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.child().sqlVal()).isEqualTo("MyChildValue");
    }

    @Test
    void shouldDeserializeGrandChildObject() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("child.child.sql_val");
        when(rs.getString(1)).thenReturn("MyChildValue");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getChild().getChild().getSqlVal()).isEqualTo("MyChildValue");
    }

    @Test
    void shouldDeserializeGrandChildObjectRecord() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("child.child.sql_val");
        when(rs.getString(1)).thenReturn("MyChildValue");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.child().child().sqlVal()).isEqualTo("MyChildValue");
    }

    @Test
    void shouldDeserializeChildObjectWithoutSetter() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("child.no_getter");
        when(rs.getString(1)).thenReturn("MyNonGettableChildValue");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getChild().getValueWithoutGetter()).isEqualTo("MyNonGettableChildValue");
    }

    @Test
    void shouldDeserializePropertyWithoutSetter() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("no_getter");
        when(rs.getString(1)).thenReturn("MyNonGettableChildValue");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getValueWithoutGetter()).isEqualTo("MyNonGettableChildValue");
    }

    @Test
    void shouldIgnoreUnknownColumnsForRecords() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        when(meta.getColumnCount()).thenReturn(2);
        when(meta.getColumnLabel(1)).thenReturn("sql_val");
        when(meta.getColumnLabel(2)).thenReturn("non_existing_prop");
        when(rs.getString(1)).thenReturn("MyValue");
        when(rs.getString(2)).thenReturn("ValueForNonExistingProp");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.sqlVal()).isEqualTo("MyValue");
    }

    @Test
    void shouldIgnoreColumnsWithoutSetters() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(2);
        when(meta.getColumnLabel(1)).thenReturn("non_existing_prop");
        when(meta.getColumnLabel(2)).thenReturn("no_getter");
        when(rs.getString(1)).thenReturn("ValueForNonExistingProp");
        when(rs.getString(2)).thenReturn("MyNonGettableChildValue");
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getValueWithoutGetter()).isEqualTo("MyNonGettableChildValue");
    }

    @Test
    void shouldDeserializeObjectWithDoubleProperty() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("double_val");
        when(rs.getDouble(1)).thenReturn(63.53d);
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getDoubleVal()).isEqualTo(63.53d);
    }

    @Test
    void shouldDeserializeObjectRecordWithDoubleProperty() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("double_val");
        when(rs.getDouble(1)).thenReturn(63.53d);
        when(rs.getMetaData()).thenReturn(meta);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.doubleVal()).isEqualTo(63.53d);
    }

    @Test
    void shouldDeserializeObjectWithNullValues() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(3);
        when(meta.getColumnLabel(1)).thenReturn("sql_val");
        when(meta.getColumnLabel(2)).thenReturn("my_bool");
        when(meta.getColumnLabel(3)).thenReturn("enum_val");
        when(rs.getString(1)).thenReturn(null);
        when(rs.getBoolean(2)).thenReturn(false);
        when(rs.getString(3)).thenReturn(null);
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.wasNull()).thenReturn(true);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getSqlVal()).isNull();
        assertThat(myTestObj.getMyBool()).isNull();
        Assertions.assertThat(myTestObj.getEnumVal()).isNull();
    }

    @Test
    void shouldDeserializeObjectRecordWithNullValues() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        when(meta.getColumnCount()).thenReturn(3);
        when(meta.getColumnLabel(1)).thenReturn("sql_val");
        when(meta.getColumnLabel(2)).thenReturn("my_bool");
        when(meta.getColumnLabel(3)).thenReturn("enum_val");
        when(rs.getString(1)).thenReturn(null);
        when(rs.getBoolean(2)).thenReturn(false);
        when(rs.getString(3)).thenReturn(null);
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.wasNull()).thenReturn(true);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.sqlVal()).isNull();
        assertThat(myTestObj.myBool()).isNull();
        Assertions.assertThat(myTestObj.enumVal()).isNull();
    }

    @Test
    void shouldDeserializeCollectionAsJson() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("list_of_strings");
        when(rs.getString(1)).thenReturn("[\"one\",\"two\"]");
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.wasNull()).thenReturn(false);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getListOfStrings()).isNotNull().hasSize(2);
        assertThat(myTestObj.getListOfStrings().getFirst()).isEqualTo("one");
        assertThat(myTestObj.getListOfStrings().get(1)).isEqualTo("two");
    }

    @Test
    void shouldDeserializeRecordCollectionAsJson() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("list_of_strings");
        when(rs.getString(1)).thenReturn("[\"one\",\"two\"]");
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.wasNull()).thenReturn(false);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.listOfStrings()).isNotNull().hasSize(2);
        assertThat(myTestObj.listOfStrings().getFirst()).isEqualTo("one");
        assertThat(myTestObj.listOfStrings().get(1)).isEqualTo("two");
    }

    @Test
    void shouldDeserializeCollectionAsArray() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnType(1)).thenReturn(Types.ARRAY);
        when(meta.getColumnLabel(1)).thenReturn("list_of_strings");
        when(jdbcArray.getArray()).thenReturn(new Object[]{"one", "two"});
        when(rs.getArray(1)).thenReturn(jdbcArray);
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.wasNull()).thenReturn(false);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getListOfStrings()).isNotNull().hasSize(2);
        assertThat(myTestObj.getListOfStrings().getFirst()).isEqualTo("one");
        assertThat(myTestObj.getListOfStrings().get(1)).isEqualTo("two");
    }

    @Test
    void shouldDeserializeRecordCollectionAsArray() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnType(1)).thenReturn(Types.ARRAY);
        when(meta.getColumnLabel(1)).thenReturn("list_of_strings");
        when(jdbcArray.getArray()).thenReturn(new Object[]{"one", "two"});
        when(rs.getArray(1)).thenReturn(jdbcArray);
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.wasNull()).thenReturn(false);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.listOfStrings()).isNotNull().hasSize(2);
        assertThat(myTestObj.listOfStrings().getFirst()).isEqualTo("one");
        assertThat(myTestObj.listOfStrings().get(1)).isEqualTo("two");
    }

    @Test
    void shouldDeserializeListOfEnums() throws SQLException {
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("list_of_enums");
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.getString(1)).thenReturn("[\"T1\", \"T2\"]");
        when(rs.wasNull()).thenReturn(false);

        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getListOfEnums()).hasSize(2);
        assertThat(myTestObj.getListOfEnums().getFirst()).isInstanceOf(TestEnum.class);
    }

    @Test
    void shouldDeserializeRecordListOfEnums() throws SQLException {
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("list_of_enums");
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.getString(1)).thenReturn("[\"T1\", \"T2\"]");
        when(rs.wasNull()).thenReturn(false);

        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.listOfEnums()).hasSize(2);
        assertThat(myTestObj.listOfEnums().getFirst()).isInstanceOf(TestEnum.class);
    }

    @Test
    void shouldDeserializeListOfObjects() throws SQLException {
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("list_of_objects");
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.getString(1)).thenReturn("[{}, {}]");
        when(rs.wasNull()).thenReturn(false);

        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getListOfObjects()).hasSize(2);
        assertThat(myTestObj.getListOfObjects().getFirst()).isInstanceOf(DbTestObj.class);
        assertThat(myTestObj.getListOfObjects().get(1)).isInstanceOf(DbTestObj.class);
    }

    @Test
    void shouldDeserializeRecordListOfObjects() throws SQLException {
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("list_of_objects");
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.getString(1)).thenReturn("[{}, {}]");
        when(rs.wasNull()).thenReturn(false);

        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.listOfObjects()).hasSize(2);
        assertThat(myTestObj.listOfObjects().getFirst()).isInstanceOf(DbTestObjRecord.class);
        assertThat(myTestObj.listOfObjects().get(1)).isInstanceOf(DbTestObjRecord.class);
    }

    @Test
    void shouldDeserializeJsonObject() throws SQLException {
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("child");
        when(meta.getColumnType(1)).thenReturn(Types.OTHER);
        when(rs.getMetaData()).thenReturn(meta);

        when(rs.getString(1)).thenReturn("{}");
        when(rs.wasNull()).thenReturn(false);

        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getChild()).isNotNull();
    }

    @Test
    void shouldDeserializeJsonObjectRecord() throws SQLException {
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("child");
        when(meta.getColumnType(1)).thenReturn(Types.OTHER);
        when(rs.getMetaData()).thenReturn(meta);

        when(rs.getString(1)).thenReturn("{}");
        when(rs.wasNull()).thenReturn(false);

        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.child()).isNotNull();
    }

    @Test
    void shouldDeserializeNullCollectionAsNull() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("list_of_strings");
        when(rs.getString(1)).thenReturn(null);
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.wasNull()).thenReturn(true);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getListOfStrings()).isNull();
    }

    @Test
    void shouldDeserializeRecordNullCollectionAsNull() throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObjRecord.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("list_of_strings");
        when(rs.getString(1)).thenReturn(null);
        when(rs.getMetaData()).thenReturn(meta);
        when(rs.wasNull()).thenReturn(true);
        DbTestObjRecord myTestObj = (DbTestObjRecord)des.deserialize(rs);
        assertThat(myTestObj.listOfStrings()).isNull();
    }

    @Test
    void shouldDeserializeArray() throws SQLException {
        when(jdbcArray.getArray()).thenReturn(new Object[]{"1"});
        when(rs.getArray(1)).thenReturn(jdbcArray);
        thenDeserialized(String[].class).isEqualTo(new String[]{"1"});

        when(rs.getArray(1)).thenReturn(null);
        thenDeserialized(String[].class).isNull();
    }

    @Test
    void shouldDeserializeByteArray() throws SQLException {
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("bytes");
        when(meta.getColumnType(1)).thenReturn(Types.BLOB);
        when(rs.getMetaData()).thenReturn(meta);

        when(rs.getBytes(1)).thenReturn("hello".getBytes());
        when(rs.wasNull()).thenReturn(false);

        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(DbTestObj.class);
        DbTestObj myTestObj = (DbTestObj)des.deserialize(rs);
        assertThat(myTestObj.getBytes()).isNotNull();
        assertThat(myTestObj.getBytes()).isEqualTo("hello".getBytes());
    }

    private ObjectAssert<?> thenDeserialized(Class<?> cls) throws SQLException {
        DbResultSetDeserializerImpl des = new DbResultSetDeserializerImpl(cls);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("colname");
        when(rs.getMetaData()).thenReturn(meta);
        return assertThat(des.deserialize(rs));
    }

    enum TestEnum {
        T1, T2, T3
    }
}
