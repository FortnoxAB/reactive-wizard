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
import java.util.*;
import java.util.function.Function;

/**
 * Creates deserializers from Strings to a given type.
 */
public class DeserializerFactory {

    private final JsonDeserializerFactory jsonDeserializerFactory;
    private final Map<Class<?>, Deserializer> stringDeserializers;

    @Inject
    public DeserializerFactory(Provider<DateFormat> dateFormatProvider, JsonDeserializerFactory jsonDeserializerFactory) {
        this.jsonDeserializerFactory = jsonDeserializerFactory;

        stringDeserializers = new HashMap<Class<?>, Deserializer>() {{
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
        }};
    }

    public DeserializerFactory() {
        this(() -> new StdDateFormat(), new JsonDeserializerFactory());
    }

    public <T> Deserializer<T> getParamDeserializer(TypeReference<T> paramType) {
        Class<?> paramCls = ReflectionUtil.getRawType(paramType.getType());
        Deserializer<T> deserializer = (Deserializer<T>) stringDeserializers.get(paramCls);

        if (deserializer != null) {
            return deserializer;
        }

        if (paramCls.isEnum()) {
            return new EnumDeserializer(paramCls);
        }

        if (List.class.isAssignableFrom(paramCls)) {
            Class<?> elementType = ReflectionUtil.getGenericParameter(paramType.getType());
            Deserializer elementDeserializer;

            if(elementType.isEnum()){
                elementDeserializer =  new EnumDeserializer(elementType);
            } else {
                elementDeserializer = stringDeserializers.get(elementType);
            }

            return new ListDeserializer(elementDeserializer);
        }

        if (paramCls.isArray()) {
            final Deserializer elementDeserializer = stringDeserializers.get(paramCls.getComponentType());
            return new ArrayDeserializer(elementDeserializer, paramCls.getComponentType());
        }

        return null;
    }

    public <T> Deserializer<T> getBodyDeserializer(TypeReference<T> paramType, String[] consumes) {
        // Only support a single consumes for now
        switch (consumes[0]) {
            case MediaType.APPLICATION_JSON:
                Function<String, T> jsonDeserializer = jsonDeserializerFactory.createDeserializer(paramType);
                return jsonDeserializer::apply;
            case MediaType.TEXT_PLAIN:
                return body -> (T) body;
        }
        return null;
    }
}
