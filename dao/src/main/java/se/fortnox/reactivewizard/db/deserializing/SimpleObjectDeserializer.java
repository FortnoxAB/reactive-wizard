package se.fortnox.reactivewizard.db.deserializing;

import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * A deserializer that uses reflection to instantiate an object and set values on it (using setters).
 */
public class SimpleObjectDeserializer {
    public static Deserializer create(Class<?> cls, ResultSetMetaData metaData) throws SQLException {
        Map<String[], PropertyDeserializer> deserializers = DeserializerUtil.createPropertyDeserializers(cls,
            metaData,
            (propertyResolver, deserializer) -> (rs, obj) -> propertyResolver.setValue(obj,
                deserializer.deserialize(rs).orElse(null)));

        return (rs) -> {
            Object object = ReflectionUtil.newInstance(cls);
            for (PropertyDeserializer propertyDeserializer : deserializers.values()) {
                propertyDeserializer.deserialize(rs, object);
            }
            return Optional.of(object);
        };
    }

    private interface PropertyDeserializer {
        void deserialize(ResultSet rs, Object obj) throws SQLException,
            InvocationTargetException, IllegalAccessException,
            InstantiationException;
    }
}
