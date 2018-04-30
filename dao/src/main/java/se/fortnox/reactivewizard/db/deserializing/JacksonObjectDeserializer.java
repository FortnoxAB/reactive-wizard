package se.fortnox.reactivewizard.db.deserializing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Deserializer that uses the conversion functionality in Jackson Databinding to build the object.
 * <p>
 * The values from the ResultSet is put into a property map (instead of for example JSON) which is then used to create
 * the POJO.
 */
public class JacksonObjectDeserializer implements Deserializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setDateFormat(new StdDateFormat());

    private final Class<?>                    targetClass;
    private final Map<String[], Deserializer> propertyDeserializers;

    private JacksonObjectDeserializer(Class<?> targetClass, Map<String[], Deserializer> propertyDeserializers) {
        this.targetClass = targetClass;
        this.propertyDeserializers = propertyDeserializers;
    }

    public static Deserializer create(Class<?> cls, ResultSetMetaData metaData) throws SQLException {
        return new JacksonObjectDeserializer(cls,
            DeserializerUtil.createPropertyDeserializers(cls, metaData, (propertyResolver, deserializer) -> deserializer));
    }

    @Override
    public Optional<?> deserialize(ResultSet rs)
        throws SQLException, InvocationTargetException, IllegalAccessException, InstantiationException {

        Map<String, Object> propertyMap = createPropertyMap(rs);
        return Optional.ofNullable(OBJECT_MAPPER.convertValue(propertyMap, targetClass));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createPropertyMap(ResultSet rs)
        throws SQLException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Map<String, Object> root = new HashMap<>();

        for (Map.Entry<String[], Deserializer> entry : propertyDeserializers.entrySet()) {
            String[]     path         = entry.getKey();
            Deserializer deserializer = entry.getValue();

            Map<String, Object> current = root;
            for (int i = 0; i < path.length; i++) {
                if (i == path.length - 1) {
                    current.put(path[i], deserializer.deserialize(rs).orElse(null));
                } else {
                    current = (Map<String, Object>)current.computeIfAbsent(path[i], k -> new HashMap<>());
                }
            }
        }
        return root;
    }
}
