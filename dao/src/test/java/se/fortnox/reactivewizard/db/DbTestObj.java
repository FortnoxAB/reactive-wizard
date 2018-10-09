package se.fortnox.reactivewizard.db;

import se.fortnox.reactivewizard.db.DbResultSetDeserializerTest.TestEnum;

import java.util.List;

public class DbTestObj {
    private String sqlVal;

    private Boolean myBool;

    private TestEnum enumVal;

    private List<String> listOfStrings;

    private List<TestEnum> listOfEnums;

    private List<DbTestObj> listOfObjects;

    private Double doubleVal;

    private DbTestObj child;

    private String    noGetterValue;

    private byte[] bytes;

    public DbTestObj() {
    }

    public String getSqlVal() {
        return sqlVal;
    }

    public void setSqlVal(String sqlVal) {
        this.sqlVal = sqlVal;
    }

    public Boolean getMyBool() {
        return myBool;
    }

    public void setMyBool(Boolean myBool) {
        this.myBool = myBool;
    }

    public TestEnum getEnumVal() {
        return enumVal;
    }

    public void setEnumVal(TestEnum enumVal) {
        this.enumVal = enumVal;
    }

    public List<String> getListOfStrings() {
        return listOfStrings;
    }

    public void setListOfStrings(List<String> listOfStrings) {
        this.listOfStrings = listOfStrings;
    }

    public Double getDoubleVal() {
        return doubleVal;
    }

    public void setDoubleVal(Double doubleVal) {
        this.doubleVal = doubleVal;
    }

    public DbTestObj getChild() {
        return child;
    }

    public void setChild(DbTestObj child) {
        this.child = child;
    }

    public void setNoGetter(String noGetterValue) {
        this.noGetterValue = noGetterValue;
    }

    public String getValueWithoutGetter() {
        return noGetterValue;
    }

    public List<TestEnum> getListOfEnums() {
        return listOfEnums;
    }

    public void setListOfEnums(List<TestEnum> listOfEnums) {
        this.listOfEnums = listOfEnums;
    }

    public List<DbTestObj> getListOfObjects() {
        return listOfObjects;
    }

    public void setListOfObjects(List<DbTestObj> listOfObjects) {
        this.listOfObjects = listOfObjects;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
