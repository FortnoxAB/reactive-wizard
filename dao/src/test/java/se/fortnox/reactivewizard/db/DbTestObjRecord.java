package se.fortnox.reactivewizard.db;

import se.fortnox.reactivewizard.db.DbResultSetDeserializerImplTest.TestEnum;

import java.util.List;

public record DbTestObjRecord(
    String sqlVal,
    Boolean myBool,
    TestEnum enumVal,
    List<String> listOfStrings,
    List<TestEnum> listOfEnums,
    List<DbTestObjRecord> listOfObjects,
    Double doubleVal,
    DbTestObjRecord child,
    byte[] bytes
) {
}
