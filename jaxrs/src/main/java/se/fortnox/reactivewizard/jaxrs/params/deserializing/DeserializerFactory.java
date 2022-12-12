package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import se.fortnox.reactivewizard.json.JsonDeserializerFactory;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.annotation.Nullable;
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

import static java.util.Map.entry;

/**
 * Creates deserializers from Strings to a given type.
 */
public class DeserializerFactory {

    private final JsonDeserializerFactory     jsonDeserializerFactory;
    private final Map<Class<?>, Deserializer<?>> stringDeserializers;

    @Inject
    public DeserializerFactory(Provider<DateFormat> dateFormatProvider, JsonDeserializerFactory jsonDeserializerFactory) {
        this.jsonDeserializerFactory = jsonDeserializerFactory;

        stringDeserializers = new HashMap<>(Map.ofEntries(
            entry(Boolean.class, new BooleanDeserializer()),
            entry(boolean.class, new BooleanNotNullDeserializer()),
            entry(int.class, new IntegerNotNullDeserializer()),
            entry(long.class, new LongNotNullDeserializer()),
            entry(double.class, new DoubleNotNullDeserializer()),
            entry(Integer.class, new IntegerDeserializer()),
            entry(Long.class, new LongDeserializer()),
            entry(Double.class, new DoubleDeserializer()),
            entry(String.class, (val) -> val),
            entry(UUID.class, new UUIDDeserializer()),
            entry(Date.class, new DateDeserializer(dateFormatProvider)),
            entry(LocalDate.class, new LocalDateDeserializer()),
            entry(LocalTime.class, new LocalTimeDeserializer())
        ));
    }

    public DeserializerFactory() {
        this(StdDateFormat::new, new JsonDeserializerFactory());
    }

    /**
     * Return Deserializer from param type.
     *
     * @param paramType the param type
     * @param <T>       the type of the deserializer
     * @return the deserializer
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Deserializer<T> getParamDeserializer(TypeReference<T> paramType) {
        var paramCls = ReflectionUtil.getRawType(paramType.getType());

        if (List.class.isAssignableFrom(paramCls)) {
            var elementCls          = ReflectionUtil.getGenericParameter(paramType.getType());
            var elementDeserializer = getClassDeserializer(elementCls);

            return new ListDeserializer(elementDeserializer);
        }

        if (paramCls.isArray()) {
            final Deserializer elementDeserializer = getClassDeserializer(paramCls.getComponentType());
            return new ArrayDeserializer(elementDeserializer, paramCls.getComponentType());
        }

        var deserializer = (Deserializer<T>)getClassDeserializer(paramCls);
        if (deserializer != null) {
            return deserializer;
        }

        throw new RuntimeException("Field of type " + paramType.getType() + " is not allowed to be used in query/form/header");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    public <T> Deserializer<T> getClassDeserializer(Class<T> paramCls) {
        var existingDeserializer = (Deserializer<T>)stringDeserializers.get(paramCls);
        if (existingDeserializer != null) {
            return existingDeserializer;
        }

        if (paramCls.isEnum()) {
            return new EnumDeserializer(paramCls);
        }

        var customClassDeserializer = CustomClassDeserializerFactory.createOrNull(paramCls);
        if (customClassDeserializer != null) {
            stringDeserializers.put(paramCls, customClassDeserializer);

            return customClassDeserializer;
        }

        return null;
    }

    /**
     * Return the body deserializer for param type.
     *
     * @param paramType the param type
     * @param consumes  the consumes requirements
     * @param <T>       type of deserializer
     * @return the deserializer
     */
    @SuppressWarnings("unchecked")
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
