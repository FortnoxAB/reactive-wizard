package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import javax.inject.Inject;
import java.util.function.Function;

/**
 * Creates instances of JSON deserializers.
 */
public class JsonDeserializerFactory {

    private final ObjectMapper mapper;

    @Inject
    public JsonDeserializerFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonDeserializerFactory() {
        this(new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }

    public <T> Function<String, T> createDeserializer(TypeReference<T> typeReference) {
        return createDeserializer(mapper.readerFor(typeReference));
    }

    public <T> Function<String, T> createDeserializer(Class<T> paramType) {
        return createDeserializer(mapper.readerFor(paramType));
    }

    private <T> Function<String, T> createDeserializer(ObjectReader reader) {
        return str -> {
            if (str == null) {
                return null;
            }
            try {
                return reader.readValue(str);
            } catch (Exception e) {
                throw new InvalidJsonException(e);
            }
        };
    }

    public <T> Function<byte[], T> createByteDeserializer(TypeReference<T> typeReference) {
        return createByteDeserializer(mapper.readerFor(typeReference));
    }

    public <T> Function<byte[], T> createByteDeserializer(Class<T> paramType) {
        return createByteDeserializer(mapper.readerFor(paramType));
    }

    private <T> Function<byte[], T> createByteDeserializer(ObjectReader reader) {
        return str -> {
            if (str == null) {
                return null;
            }
            try {
                return reader.readValue(str);
            } catch (Exception e) {
                throw new InvalidJsonException(e);
            }
        };
    }

}
