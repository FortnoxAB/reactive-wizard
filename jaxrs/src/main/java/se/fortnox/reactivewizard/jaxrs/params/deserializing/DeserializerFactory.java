package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import se.fortnox.reactivewizard.json.JsonDeserializerFactory;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.MediaType;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Creates deserializers from Strings to a given type.
 */
public class DeserializerFactory {

    private final JsonDeserializerFactory     jsonDeserializerFactory;
    private final Map<Class<?>, Deserializer> stringDeserializers;

    @Inject
    public DeserializerFactory(Provider<DateFormat> dateFormatProvider, JsonDeserializerFactory jsonDeserializerFactory) {
        this.jsonDeserializerFactory = jsonDeserializerFactory;

        stringDeserializers = new HashMap<Class<?>, Deserializer>() {
            {
                put(Boolean.class, new BooleanDeserializer());
                put(boolean.class, new BooleanNotNullDeserializer());
                put(int.class, new IntegerNotNullDeserializer());
                put(long.class, new LongNotNullDeserializer());
                put(double.class, new DoubleNotNullDeserializer());
                put(Integer.class, new IntegerDeserializer());
                put(Long.class, new LongDeserializer());
                put(Double.class, new DoubleDeserializer());
                put(String.class, (val) -> val);
                put(UUID.class, new UUIDDeserializer());
                put(Date.class, new DateDeserializer(dateFormatProvider));
                put(LocalDate.class, new LocalDateDeserializer());
                put(LocalTime.class, new LocalTimeDeserializer());
            }
        };
    }

    public DeserializerFactory() {
        this(() -> new StdDateFormat(), new JsonDeserializerFactory());
    }

    /**
     * Return Deserializer from param type.
      * @param paramType the param type
     * @param <T> the type of the deserializer
     * @return the deserializer
     */
    public <T> Deserializer<T> getParamDeserializer(TypeReference<T> paramType) {
        Class<?>        paramCls     = ReflectionUtil.getRawType(paramType.getType());
        Deserializer<T> deserializer = (Deserializer<T>)stringDeserializers.get(paramCls);

        if (deserializer != null) {
            return deserializer;
        }

        if (paramCls.isEnum()) {
            return new EnumDeserializer(paramCls);
        }

        if (List.class.isAssignableFrom(paramCls)) {
            Class<?>     elementType = ReflectionUtil.getGenericParameter(paramType.getType());
            Deserializer elementDeserializer;

            if (elementType.isEnum()) {
                elementDeserializer = new EnumDeserializer(elementType);
            } else {
                elementDeserializer = stringDeserializers.get(elementType);
            }

            return new ListDeserializer(elementDeserializer);
        }

        if (paramCls.isArray()) {
            final Deserializer elementDeserializer = stringDeserializers.get(paramCls.getComponentType());
            return new ArrayDeserializer(elementDeserializer, paramCls.getComponentType());
        }

        throw new RuntimeException("Field of type " + paramType.getType() + " is not allowed to be used in query/form/header");
    }

    /**
     * Return the body deserializer for param type.
     * @param paramType the param type
     * @param consumes the consumes requirements
     * @param <T> type of deserializer
     * @return the deserializer
     */
    public <T> BodyDeserializer<T> getBodyDeserializer(TypeReference<T> paramType, String[] consumes) {
        // Only support a single consumes for now
        String consume = consumes[0];

        if (MediaType.APPLICATION_JSON.equals(consume)) {
            Function<byte[], T> jsonDeserializer = jsonDeserializerFactory.createByteDeserializer(paramType);
            return jsonDeserializer::apply;
        } else if (MediaType.TEXT_PLAIN.equals(consume) || MediaType.APPLICATION_OCTET_STREAM.equals(consume)) {
            if (paramType.getType().equals(String.class)) {
                return bytes -> (T)new String(bytes);
            }
            return bytes -> (T)bytes;
        } else if (paramType.getType().equals(byte[].class)) {
            return bytes -> (T)bytes;
        }
        return null;
    }
}
