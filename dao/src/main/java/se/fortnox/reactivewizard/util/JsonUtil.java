package se.fortnox.reactivewizard.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.lang.reflect.Type;
import java.util.Map;

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setDateFormat(new StdDateFormat());

    public static String json(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(ObjectReader reader, String string) {
        if (string == null) {
            return null;
        }
        T obj;
        try {
            obj = reader.readValue(string);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing: " + string, e);
        }
        return obj;
    }

    public static ObjectReader reader(Type type) {
        return mapper.readerFor(TypeFactory.defaultInstance().constructType(type));
    }

    /**
     * Convert a property map into a POJO.
     *
     * @param propertyMap The map containing the values describing the object.
     * @param targetClass The POJO class.
     * @param <T>         The POJO type.
     * @return An instance of the POJO class with the property values from the map.
     */
    public static <T> T convertValue(Map<String, Object> propertyMap, Class<T> targetClass) {
        return mapper.convertValue(propertyMap, targetClass);
    }
}
