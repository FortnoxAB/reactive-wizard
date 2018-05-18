package se.fortnox.reactivewizard.db.deserializing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.json.JsonDeserializerFactory;
import se.fortnox.reactivewizard.util.CamelSnakeConverter;
import se.fortnox.reactivewizard.util.ReflectionUtil;
import se.fortnox.reactivewizard.util.rx.PropertyResolver;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class DeserializerUtil {
    private static final Logger                  LOG                       = LoggerFactory.getLogger(DeserializerUtil.class);
    private static final JsonDeserializerFactory JSON_DESERIALIZER_FACTORY = new JsonDeserializerFactory();

    static <I,T> Map<String[], T> createPropertyDeserializers(Class<I> cls, ResultSetMetaData metaData,
        BiFunction<PropertyResolver<I,?>, Deserializer, T> deserializerFactory
    ) throws SQLException {
        Map<String[], T> propertyDeserializers = new LinkedHashMap<>();

        String[] columns = extractColumnLabels(metaData);
        for (int i = 0; i < columns.length; i++) {
            String   column       = columns[i];
            String   propertyName = CamelSnakeConverter.snakeToCamel(column);
            String[] propertyPath = propertyName.split("\\.");

            Optional<PropertyResolver> propertyResolver = ReflectionUtil.getPropertyResolver(cls, propertyPath);
            if (propertyResolver.isPresent()) {
                T propertyDeserializer = createPropertyDeserializer((PropertyResolver<I, ?>) propertyResolver.get(), metaData, i + 1, deserializerFactory);
                propertyDeserializers.put(propertyPath, propertyDeserializer);
            } else {
                LOG.warn("Tried to deserialize column " + column + ", but found no matching property named " + propertyName + " in " + cls.getSimpleName());
            }
        }
        return Collections.unmodifiableMap(propertyDeserializers);
    }

    private static <I,T> T createPropertyDeserializer(PropertyResolver<I,?> propertyResolver, ResultSetMetaData metaData,
        int columnIndex, BiFunction<PropertyResolver<I,?>, Deserializer, T> deserializerFactory
    ) throws SQLException {
        Class<?> type       = propertyResolver.getPropertyType();
        int      columnType = metaData.getColumnType(columnIndex);

        Deserializer simpleProp = ColumnDeserializerFactory.getColumnDeserializer(type, columnType, columnIndex);
        if (simpleProp == null) {
            return jsonPropertyDeserializer(propertyResolver, columnIndex, deserializerFactory, type);
        }

        return deserializerFactory.apply(propertyResolver, simpleProp);
    }

    private static <I,T> T jsonPropertyDeserializer(PropertyResolver<I,?> propertyResolver,
        int columnIndex,
        BiFunction<PropertyResolver<I,?>, Deserializer, T> deserializerFactory,
        Class<?> type
    ) {
        Function<String, ?> deserializer = JSON_DESERIALIZER_FACTORY.createDeserializer(type);
        return deserializerFactory.apply(propertyResolver, (rs) -> {
            String columnValue = rs.getString(columnIndex);
            if (columnValue == null) {
                return Optional.empty();
            }
            Object value       = deserializer.apply(columnValue);
            return Optional.ofNullable(value);
        });
    }

    private static String[] extractColumnLabels(ResultSetMetaData metaData) throws SQLException {
        String[] columnLabels = new String[metaData.getColumnCount()];
        for (int i = 0; i < columnLabels.length; i++) {
            columnLabels[i] = metaData.getColumnLabel(i + 1);
        }
        return columnLabels;
    }

}
