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
import java.util.function.BiFunction;

class DeserializerUtil {
    private static final Logger                  LOG                       = LoggerFactory.getLogger(DeserializerUtil.class);
    private static final JsonDeserializerFactory JSON_DESERIALIZER_FACTORY = new JsonDeserializerFactory();

    static <T> Map<String[], T> createPropertyDeserializers(Class<?> cls, ResultSetMetaData metaData,
        BiFunction<PropertyResolver, Deserializer, T> deserializerFactory
    ) throws SQLException {
        Map<String[], T> propertyDeserializers = new LinkedHashMap<>();

        String[] columns = extractColumnLabels(metaData);
        for (int i = 0; i < columns.length; i++) {
            String   column       = columns[i];
            String   propertyName = CamelSnakeConverter.snakeToCamel(column);
            String[] propertyPath = propertyName.split("\\.");

            Optional<PropertyResolver> propertyResolver = ReflectionUtil.getPropertyResolver(cls, propertyPath);
            if (propertyResolver.isPresent()) {
                T propertyDeserializer = createPropertyDeserializer(propertyResolver.get(), metaData, i + 1, deserializerFactory);
                propertyDeserializers.put(propertyPath, propertyDeserializer);
            } else {
                LOG.warn("Tried to deserialize column " + column + ", but found no matching property named " + propertyName + " in " + cls.getSimpleName());
            }
        }
        return Collections.unmodifiableMap(propertyDeserializers);
    }

    private static <T> T createPropertyDeserializer(PropertyResolver propertyResolver, ResultSetMetaData metaData,
        int columnIndex, BiFunction<PropertyResolver, Deserializer, T> deserializerFactory
    ) throws SQLException {
        Class<?> type       = propertyResolver.getPropertyType();
        int      columnType = metaData.getColumnType(columnIndex);

        Deserializer simpleProp = ColumnDeserializerFactory.getColumnDeserializer(type, columnType, columnIndex);
        if (simpleProp == null) {
            return jsonPropertyDeserializer(propertyResolver, columnIndex, deserializerFactory, type);
        }

        return deserializerFactory.apply(propertyResolver, simpleProp);
    }

    private static <T> T jsonPropertyDeserializer(PropertyResolver propertyResolver,
        int columnIndex,
        BiFunction<PropertyResolver, Deserializer, T> deserializerFactory,
        Class<?> type
    ) {
        return deserializerFactory.apply(propertyResolver, (rs) -> {
            String columnValue = rs.getString(columnIndex);
            Object value       = JSON_DESERIALIZER_FACTORY.createDeserializer(type).apply(columnValue);
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
