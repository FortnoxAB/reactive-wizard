package se.fortnox.reactivewizard.db.deserializing;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

interface Deserializer {
    Optional<?> deserialize(ResultSet rs) throws SQLException,
        InvocationTargetException, IllegalAccessException,
        InstantiationException;
}
