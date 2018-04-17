package se.fortnox.reactivewizard.db;

import se.fortnox.reactivewizard.db.DbResultSetDeserializerTest.TestEnum;

import java.util.List;

public class ImmutableDbTestObj {
    private final String             sqlVal;
    private final Boolean            myBool;
    private final TestEnum           enumVal;
    private final List<String>       listOfStrings;
    private final Double             doubleVal;
    private final ImmutableDbTestObj child;
    private final String             noGetter;

    public ImmutableDbTestObj(String sqlVal, Boolean myBool, TestEnum enumVal, List<String> listOfStrings, Double doubleVal,
        ImmutableDbTestObj child, String noGetter
    ) {
        this.sqlVal = sqlVal;
        this.myBool = myBool;
        this.enumVal = enumVal;
        this.listOfStrings = listOfStrings;
        this.doubleVal = doubleVal;
        this.child = child;
        this.noGetter = noGetter;
    }

    public String getSqlVal() {
        return sqlVal;
    }

    public Boolean getMyBool() {
        return myBool;
    }

    public TestEnum getEnumVal() {
        return enumVal;
    }

    public List<String> getListOfStrings() {
        return listOfStrings;
    }

    public Double getDoubleVal() {
        return doubleVal;
    }

    public ImmutableDbTestObj getChild() {
        return child;
    }

    public String getValueWithoutGetter() {
        return noGetter;
    }
}
