package se.fortnox.reactivewizard.db.deserializing;

import se.fortnox.reactivewizard.util.PropertyResolver;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A deserializer that uses reflection to instantiate an object and set values on it (using setters).
 */
public class SimpleObjectDeserializer {

    /**
     * Create a deserializer.
     * @param cls the class
     * @param metaData the meta data
     * @param <I> the type of deserializer
     * @return the deserializer
     * @throws SQLException on error
     */
    public static <I> Deserializer<I> create(Class<I> cls, ResultSetMetaData metaData) throws SQLException {
        Map<String[], PropertyDeserializer> deserializers = DeserializerUtil.createPropertyDeserializers(
            cls,
            metaData,
            SimpleObjectDeserializer::createRecordPropertyDeserializer);

        Supplier<I> instantiator = ReflectionUtil.instantiator(cls);

        return (rs) -> {
            I object = instantiator.get();
            for (PropertyDeserializer propertyDeserializer : deserializers.values()) {
                propertyDeserializer.deserialize(rs, object);
            }
            return Optional.of(object);
        };
    }

    private static <I,T> PropertyDeserializer<I> createRecordPropertyDeserializer(
        PropertyResolver<I,T> propertyResolver,
        Deserializer<T> deserializer) {
        BiConsumer<I, T> setter = propertyResolver.setter();
        return (rs, obj) -> setter.accept(obj, deserializer.deserialize(rs).orElse(null));
    }

    private interface PropertyDeserializer<I> {
        void deserialize(ResultSet rs, I obj) throws SQLException,
            InvocationTargetException, IllegalAccessException,
            InstantiationException;
    }
}
