package se.fortnox.reactivewizard.db.deserializing;

import org.junit.Test;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;

public class DeserializationPerformanceTest {
    @Test
    public void testPerformance() {
        DbResultSetDeserializer des = new DbResultSetDeserializer(PersonWithAddress.class);

        ResultSetMetaData metadata = new MockResultSetMetaData();
        ResultSet resultSet = new MockedResultSet(1, metadata);
        PersonWithAddress deserialized = (PersonWithAddress)des.deserialize(resultSet);
        assertThat(deserialized.getId()).isEqualTo(UUID.fromString("7aa24e00-4fff-42b4-8cf1-8bcf43f09b8b"));
        assertThat(deserialized.getFirstName()).isEqualTo("John");
        assertThat(deserialized.getLastName()).isEqualTo("Doe");
        assertThat(deserialized.getMiddleName()).isEqualTo("Middle");
        assertThat(deserialized.getAge()).isEqualTo(10);
        assertThat(deserialized.getCoAddress()).isNull();
        assertThat(deserialized.getStreetAddress()).isEqualTo("Streetname");
        assertThat(deserialized.getStreetAddressNo()).isEqualTo(7);
        assertThat(deserialized.getZipCode()).isEqualTo("99900");
        assertThat(deserialized.getPostalCity()).isEqualTo("City");
        assertThat(deserialized.getPhoneNumber()).isEqualTo("1234567890");
        assertThat(deserialized.getMobileNumber()).isEqualTo("1234567890");
        assertThat(deserialized.getFaxNumber()).isEqualTo("123456789");
        assertThat(deserialized.getEmail()).isEqualTo("email@localhost");
        assertThat(deserialized.getGender()).isEqualTo(PersonWithAddress.Gender.FEMALE);
        assertThat(deserialized.getPasswordDate()).isEqualTo(LocalDate.parse("2010-01-01"));


        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000000; i++) {
            des.deserialize(new MockedResultSet(1, metadata));
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime-startTime;
        System.out.println("Duration: "+duration);
        assertThat(duration).isLessThan(1000);

    }


    public static class PersonWithAddress {
        private UUID id;
        private String firstName;
        private String lastName;
        private String middleName;
        private Integer age;
        private String coAddress;
        private String streetAddress;
        private Integer streetAddressNo;
        private String zipCode;
        private String postalCity;
        private String phoneNumber;
        private String mobileNumber;
        private String faxNumber;
        private String email;
        private Gender gender;
        private boolean certified;
        private LocalDate passwordDate;

        public enum Gender {
            MALE, FEMALE
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getMiddleName() {
            return middleName;
        }

        public void setMiddleName(String middleName) {
            this.middleName = middleName;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getCoAddress() {
            return coAddress;
        }

        public void setCoAddress(String coAddress) {
            this.coAddress = coAddress;
        }

        public String getStreetAddress() {
            return streetAddress;
        }

        public void setStreetAddress(String streetAddress) {
            this.streetAddress = streetAddress;
        }

        public Integer getStreetAddressNo() {
            return streetAddressNo;
        }

        public void setStreetAddressNo(Integer streetAddressNo) {
            this.streetAddressNo = streetAddressNo;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }

        public String getPostalCity() {
            return postalCity;
        }

        public void setPostalCity(String postalCity) {
            this.postalCity = postalCity;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getMobileNumber() {
            return mobileNumber;
        }

        public void setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
        }

        public String getFaxNumber() {
            return faxNumber;
        }

        public void setFaxNumber(String faxNumber) {
            this.faxNumber = faxNumber;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Gender getGender() {
            return gender;
        }

        public void setGender(Gender gender) {
            this.gender = gender;
        }

        public boolean isCertified() {
            return certified;
        }

        public void setCertified(boolean certified) {
            this.certified = certified;
        }

        public LocalDate getPasswordDate() {
            return passwordDate;
        }

        public void setPasswordDate(LocalDate passwordDate) {
            this.passwordDate = passwordDate;
        }
    }
    
    public static class MockResultSetMetaData implements ResultSetMetaData {

        private String[] columnNames = new String[]{
                "",
        "id",
        "first_name",
        "last_name",
        "middle_name",
        "age",
        "co_address",
        "street_address",
        "street_address_no",
        "zip_code",
        "postal_city",
        "phone_number",
        "mobile_number",
        "fax_number",
        "email",
        "gender",
        "certified",
        "password_date"
        };
        private int[] columnTypes = new int[]{
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR
        };

        @Override
        public int getColumnCount() throws SQLException {
            return 17;
        }

        @Override
        public boolean isAutoIncrement(int i) throws SQLException {
            return false;
        }

        @Override
        public boolean isCaseSensitive(int i) throws SQLException {
            return false;
        }

        @Override
        public boolean isSearchable(int i) throws SQLException {
            return false;
        }

        @Override
        public boolean isCurrency(int i) throws SQLException {
            return false;
        }

        @Override
        public int isNullable(int i) throws SQLException {
            return 0;
        }

        @Override
        public boolean isSigned(int i) throws SQLException {
            return false;
        }

        @Override
        public int getColumnDisplaySize(int i) throws SQLException {
            return 0;
        }

        @Override
        public String getColumnLabel(int i) throws SQLException {
            return columnNames[i];
        }

        @Override
        public String getColumnName(int i) throws SQLException {
            return null;
        }

        @Override
        public String getSchemaName(int i) throws SQLException {
            return null;
        }

        @Override
        public int getPrecision(int i) throws SQLException {
            return 0;
        }

        @Override
        public int getScale(int i) throws SQLException {
            return 0;
        }

        @Override
        public String getTableName(int i) throws SQLException {
            return null;
        }

        @Override
        public String getCatalogName(int i) throws SQLException {
            return null;
        }

        @Override
        public int getColumnType(int i) throws SQLException {
            return columnTypes[i];
        }

        @Override
        public String getColumnTypeName(int i) throws SQLException {
            return null;
        }

        @Override
        public boolean isReadOnly(int i) throws SQLException {
            return false;
        }

        @Override
        public boolean isWritable(int i) throws SQLException {
            return false;
        }

        @Override
        public boolean isDefinitelyWritable(int i) throws SQLException {
            return false;
        }

        @Override
        public String getColumnClassName(int i) throws SQLException {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> aClass) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> aClass) throws SQLException {
            return false;
        }
    }

    public static class MockedResultSet implements ResultSet {

        private final int limit;
        private int count;
        private ResultSetMetaData metadata;
        private String[] stringValues = new String[]{
                null,
                null,
                "John",
                "Doe",
                "Middle",
                "",
                null,
                "Streetname",
                null,
                "99900",
                "City",
                "1234567890",
                "1234567890",
                "123456789",
                "email@localhost",
                "FEMALE",
                "",
                "",
                "",
                "",
        };
        private int[] ints = new int[]{
                0,
                0,
                0,
                0,
                0,
                10,
                0,
                0,
                7,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
        };

        public MockedResultSet(int limit, ResultSetMetaData metadata) {
            this.limit = limit;
            this.metadata = metadata;
        }

        @Override
        public boolean next() throws SQLException {
            count++;
            return count < limit;
        }

        @Override
        public void close() throws SQLException {

        }

        @Override
        public boolean wasNull() throws SQLException {
            return false;
        }

        @Override
        public String getString(int i) throws SQLException {
            return stringValues[i];
        }

        @Override
        public boolean getBoolean(int i) throws SQLException {
            return false;
        }

        @Override
        public byte getByte(int i) throws SQLException {
            return 0;
        }

        @Override
        public short getShort(int i) throws SQLException {
            return 0;
        }

        @Override
        public int getInt(int i) throws SQLException {
            return ints[i];
        }

        @Override
        public long getLong(int i) throws SQLException {
            return 0;
        }

        @Override
        public float getFloat(int i) throws SQLException {
            return 0;
        }

        @Override
        public double getDouble(int i) throws SQLException {
            return 0;
        }

        @Override
        public BigDecimal getBigDecimal(int i, int i1) throws SQLException {
            return null;
        }

        @Override
        public byte[] getBytes(int i) throws SQLException {
            return new byte[0];
        }

        @Override
        public Date getDate(int i) throws SQLException {
            return i == 17 ? new Date(1262300400000L) : null;
        }

        @Override
        public Time getTime(int i) throws SQLException {
            return null;
        }

        @Override
        public Timestamp getTimestamp(int i) throws SQLException {
            return null;
        }

        @Override
        public InputStream getAsciiStream(int i) throws SQLException {
            return null;
        }

        @Override
        public InputStream getUnicodeStream(int i) throws SQLException {
            return null;
        }

        @Override
        public InputStream getBinaryStream(int i) throws SQLException {
            return null;
        }

        @Override
        public String getString(String s) throws SQLException {
            return null;
        }

        @Override
        public boolean getBoolean(String s) throws SQLException {
            return false;
        }

        @Override
        public byte getByte(String s) throws SQLException {
            return 0;
        }

        @Override
        public short getShort(String s) throws SQLException {
            return 0;
        }

        @Override
        public int getInt(String s) throws SQLException {
            return 0;
        }

        @Override
        public long getLong(String s) throws SQLException {
            return 0;
        }

        @Override
        public float getFloat(String s) throws SQLException {
            return 0;
        }

        @Override
        public double getDouble(String s) throws SQLException {
            return 0;
        }

        @Override
        public BigDecimal getBigDecimal(String s, int i) throws SQLException {
            return null;
        }

        @Override
        public byte[] getBytes(String s) throws SQLException {
            return new byte[0];
        }

        @Override
        public Date getDate(String s) throws SQLException {
            return null;
        }

        @Override
        public Time getTime(String s) throws SQLException {
            return null;
        }

        @Override
        public Timestamp getTimestamp(String s) throws SQLException {
            return null;
        }

        @Override
        public InputStream getAsciiStream(String s) throws SQLException {
            return null;
        }

        @Override
        public InputStream getUnicodeStream(String s) throws SQLException {
            return null;
        }

        @Override
        public InputStream getBinaryStream(String s) throws SQLException {
            return null;
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return null;
        }

        @Override
        public void clearWarnings() throws SQLException {

        }

        @Override
        public String getCursorName() throws SQLException {
            return null;
        }

        @Override
        public ResultSetMetaData getMetaData() throws SQLException {
            return metadata;
        }

        @Override
        public Object getObject(int i) throws SQLException {
            return i == 1 ? UUID.fromString("7aa24e00-4fff-42b4-8cf1-8bcf43f09b8b") : null;
        }

        @Override
        public Object getObject(String s) throws SQLException {
            return null;
        }

        @Override
        public int findColumn(String s) throws SQLException {
            return 0;
        }

        @Override
        public Reader getCharacterStream(int i) throws SQLException {
            return null;
        }

        @Override
        public Reader getCharacterStream(String s) throws SQLException {
            return null;
        }

        @Override
        public BigDecimal getBigDecimal(int i) throws SQLException {
            return null;
        }

        @Override
        public BigDecimal getBigDecimal(String s) throws SQLException {
            return null;
        }

        @Override
        public boolean isBeforeFirst() throws SQLException {
            return false;
        }

        @Override
        public boolean isAfterLast() throws SQLException {
            return false;
        }

        @Override
        public boolean isFirst() throws SQLException {
            return false;
        }

        @Override
        public boolean isLast() throws SQLException {
            return false;
        }

        @Override
        public void beforeFirst() throws SQLException {

        }

        @Override
        public void afterLast() throws SQLException {

        }

        @Override
        public boolean first() throws SQLException {
            return false;
        }

        @Override
        public boolean last() throws SQLException {
            return false;
        }

        @Override
        public int getRow() throws SQLException {
            return 0;
        }

        @Override
        public boolean absolute(int i) throws SQLException {
            return false;
        }

        @Override
        public boolean relative(int i) throws SQLException {
            return false;
        }

        @Override
        public boolean previous() throws SQLException {
            return false;
        }

        @Override
        public void setFetchDirection(int i) throws SQLException {

        }

        @Override
        public int getFetchDirection() throws SQLException {
            return 0;
        }

        @Override
        public void setFetchSize(int i) throws SQLException {

        }

        @Override
        public int getFetchSize() throws SQLException {
            return 0;
        }

        @Override
        public int getType() throws SQLException {
            return 0;
        }

        @Override
        public int getConcurrency() throws SQLException {
            return 0;
        }

        @Override
        public boolean rowUpdated() throws SQLException {
            return false;
        }

        @Override
        public boolean rowInserted() throws SQLException {
            return false;
        }

        @Override
        public boolean rowDeleted() throws SQLException {
            return false;
        }

        @Override
        public void updateNull(int i) throws SQLException {

        }

        @Override
        public void updateBoolean(int i, boolean b) throws SQLException {

        }

        @Override
        public void updateByte(int i, byte b) throws SQLException {

        }

        @Override
        public void updateShort(int i, short i1) throws SQLException {

        }

        @Override
        public void updateInt(int i, int i1) throws SQLException {

        }

        @Override
        public void updateLong(int i, long l) throws SQLException {

        }

        @Override
        public void updateFloat(int i, float v) throws SQLException {

        }

        @Override
        public void updateDouble(int i, double v) throws SQLException {

        }

        @Override
        public void updateBigDecimal(int i, BigDecimal bigDecimal) throws SQLException {

        }

        @Override
        public void updateString(int i, String s) throws SQLException {

        }

        @Override
        public void updateBytes(int i, byte[] bytes) throws SQLException {

        }

        @Override
        public void updateDate(int i, Date date) throws SQLException {

        }

        @Override
        public void updateTime(int i, Time time) throws SQLException {

        }

        @Override
        public void updateTimestamp(int i, Timestamp timestamp) throws SQLException {

        }

        @Override
        public void updateAsciiStream(int i, InputStream inputStream, int i1) throws SQLException {

        }

        @Override
        public void updateBinaryStream(int i, InputStream inputStream, int i1) throws SQLException {

        }

        @Override
        public void updateCharacterStream(int i, Reader reader, int i1) throws SQLException {

        }

        @Override
        public void updateObject(int i, Object o, int i1) throws SQLException {

        }

        @Override
        public void updateObject(int i, Object o) throws SQLException {

        }

        @Override
        public void updateNull(String s) throws SQLException {

        }

        @Override
        public void updateBoolean(String s, boolean b) throws SQLException {

        }

        @Override
        public void updateByte(String s, byte b) throws SQLException {

        }

        @Override
        public void updateShort(String s, short i) throws SQLException {

        }

        @Override
        public void updateInt(String s, int i) throws SQLException {

        }

        @Override
        public void updateLong(String s, long l) throws SQLException {

        }

        @Override
        public void updateFloat(String s, float v) throws SQLException {

        }

        @Override
        public void updateDouble(String s, double v) throws SQLException {

        }

        @Override
        public void updateBigDecimal(String s, BigDecimal bigDecimal) throws SQLException {

        }

        @Override
        public void updateString(String s, String s1) throws SQLException {

        }

        @Override
        public void updateBytes(String s, byte[] bytes) throws SQLException {

        }

        @Override
        public void updateDate(String s, Date date) throws SQLException {

        }

        @Override
        public void updateTime(String s, Time time) throws SQLException {

        }

        @Override
        public void updateTimestamp(String s, Timestamp timestamp) throws SQLException {

        }

        @Override
        public void updateAsciiStream(String s, InputStream inputStream, int i) throws SQLException {

        }

        @Override
        public void updateBinaryStream(String s, InputStream inputStream, int i) throws SQLException {

        }

        @Override
        public void updateCharacterStream(String s, Reader reader, int i) throws SQLException {

        }

        @Override
        public void updateObject(String s, Object o, int i) throws SQLException {

        }

        @Override
        public void updateObject(String s, Object o) throws SQLException {

        }

        @Override
        public void insertRow() throws SQLException {

        }

        @Override
        public void updateRow() throws SQLException {

        }

        @Override
        public void deleteRow() throws SQLException {

        }

        @Override
        public void refreshRow() throws SQLException {

        }

        @Override
        public void cancelRowUpdates() throws SQLException {

        }

        @Override
        public void moveToInsertRow() throws SQLException {

        }

        @Override
        public void moveToCurrentRow() throws SQLException {

        }

        @Override
        public Statement getStatement() throws SQLException {
            return null;
        }

        @Override
        public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        @Override
        public Ref getRef(int i) throws SQLException {
            return null;
        }

        @Override
        public Blob getBlob(int i) throws SQLException {
            return null;
        }

        @Override
        public Clob getClob(int i) throws SQLException {
            return null;
        }

        @Override
        public Array getArray(int i) throws SQLException {
            return null;
        }

        @Override
        public Object getObject(String s, Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        @Override
        public Ref getRef(String s) throws SQLException {
            return null;
        }

        @Override
        public Blob getBlob(String s) throws SQLException {
            return null;
        }

        @Override
        public Clob getClob(String s) throws SQLException {
            return null;
        }

        @Override
        public Array getArray(String s) throws SQLException {
            return null;
        }

        @Override
        public Date getDate(int i, Calendar calendar) throws SQLException {
            return null;
        }

        @Override
        public Date getDate(String s, Calendar calendar) throws SQLException {
            return null;
        }

        @Override
        public Time getTime(int i, Calendar calendar) throws SQLException {
            return null;
        }

        @Override
        public Time getTime(String s, Calendar calendar) throws SQLException {
            return null;
        }

        @Override
        public Timestamp getTimestamp(int i, Calendar calendar) throws SQLException {
            return null;
        }

        @Override
        public Timestamp getTimestamp(String s, Calendar calendar) throws SQLException {
            return null;
        }

        @Override
        public URL getURL(int i) throws SQLException {
            return null;
        }

        @Override
        public URL getURL(String s) throws SQLException {
            return null;
        }

        @Override
        public void updateRef(int i, Ref ref) throws SQLException {

        }

        @Override
        public void updateRef(String s, Ref ref) throws SQLException {

        }

        @Override
        public void updateBlob(int i, Blob blob) throws SQLException {

        }

        @Override
        public void updateBlob(String s, Blob blob) throws SQLException {

        }

        @Override
        public void updateClob(int i, Clob clob) throws SQLException {

        }

        @Override
        public void updateClob(String s, Clob clob) throws SQLException {

        }

        @Override
        public void updateArray(int i, Array array) throws SQLException {

        }

        @Override
        public void updateArray(String s, Array array) throws SQLException {

        }

        @Override
        public RowId getRowId(int i) throws SQLException {
            return null;
        }

        @Override
        public RowId getRowId(String s) throws SQLException {
            return null;
        }

        @Override
        public void updateRowId(int i, RowId rowId) throws SQLException {

        }

        @Override
        public void updateRowId(String s, RowId rowId) throws SQLException {

        }

        @Override
        public int getHoldability() throws SQLException {
            return 0;
        }

        @Override
        public boolean isClosed() throws SQLException {
            return false;
        }

        @Override
        public void updateNString(int i, String s) throws SQLException {

        }

        @Override
        public void updateNString(String s, String s1) throws SQLException {

        }

        @Override
        public void updateNClob(int i, NClob nClob) throws SQLException {

        }

        @Override
        public void updateNClob(String s, NClob nClob) throws SQLException {

        }

        @Override
        public NClob getNClob(int i) throws SQLException {
            return null;
        }

        @Override
        public NClob getNClob(String s) throws SQLException {
            return null;
        }

        @Override
        public SQLXML getSQLXML(int i) throws SQLException {
            return null;
        }

        @Override
        public SQLXML getSQLXML(String s) throws SQLException {
            return null;
        }

        @Override
        public void updateSQLXML(int i, SQLXML sqlxml) throws SQLException {

        }

        @Override
        public void updateSQLXML(String s, SQLXML sqlxml) throws SQLException {

        }

        @Override
        public String getNString(int i) throws SQLException {
            return null;
        }

        @Override
        public String getNString(String s) throws SQLException {
            return null;
        }

        @Override
        public Reader getNCharacterStream(int i) throws SQLException {
            return null;
        }

        @Override
        public Reader getNCharacterStream(String s) throws SQLException {
            return null;
        }

        @Override
        public void updateNCharacterStream(int i, Reader reader, long l) throws SQLException {

        }

        @Override
        public void updateNCharacterStream(String s, Reader reader, long l) throws SQLException {

        }

        @Override
        public void updateAsciiStream(int i, InputStream inputStream, long l) throws SQLException {

        }

        @Override
        public void updateBinaryStream(int i, InputStream inputStream, long l) throws SQLException {

        }

        @Override
        public void updateCharacterStream(int i, Reader reader, long l) throws SQLException {

        }

        @Override
        public void updateAsciiStream(String s, InputStream inputStream, long l) throws SQLException {

        }

        @Override
        public void updateBinaryStream(String s, InputStream inputStream, long l) throws SQLException {

        }

        @Override
        public void updateCharacterStream(String s, Reader reader, long l) throws SQLException {

        }

        @Override
        public void updateBlob(int i, InputStream inputStream, long l) throws SQLException {

        }

        @Override
        public void updateBlob(String s, InputStream inputStream, long l) throws SQLException {

        }

        @Override
        public void updateClob(int i, Reader reader, long l) throws SQLException {

        }

        @Override
        public void updateClob(String s, Reader reader, long l) throws SQLException {

        }

        @Override
        public void updateNClob(int i, Reader reader, long l) throws SQLException {

        }

        @Override
        public void updateNClob(String s, Reader reader, long l) throws SQLException {

        }

        @Override
        public void updateNCharacterStream(int i, Reader reader) throws SQLException {

        }

        @Override
        public void updateNCharacterStream(String s, Reader reader) throws SQLException {

        }

        @Override
        public void updateAsciiStream(int i, InputStream inputStream) throws SQLException {

        }

        @Override
        public void updateBinaryStream(int i, InputStream inputStream) throws SQLException {

        }

        @Override
        public void updateCharacterStream(int i, Reader reader) throws SQLException {

        }

        @Override
        public void updateAsciiStream(String s, InputStream inputStream) throws SQLException {

        }

        @Override
        public void updateBinaryStream(String s, InputStream inputStream) throws SQLException {

        }

        @Override
        public void updateCharacterStream(String s, Reader reader) throws SQLException {

        }

        @Override
        public void updateBlob(int i, InputStream inputStream) throws SQLException {

        }

        @Override
        public void updateBlob(String s, InputStream inputStream) throws SQLException {

        }

        @Override
        public void updateClob(int i, Reader reader) throws SQLException {

        }

        @Override
        public void updateClob(String s, Reader reader) throws SQLException {

        }

        @Override
        public void updateNClob(int i, Reader reader) throws SQLException {

        }

        @Override
        public void updateNClob(String s, Reader reader) throws SQLException {

        }

        @Override
        public <T> T getObject(int i, Class<T> aClass) throws SQLException {
            return null;
        }

        @Override
        public <T> T getObject(String s, Class<T> aClass) throws SQLException {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> aClass) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> aClass) throws SQLException {
            return false;
        }
    }

}
