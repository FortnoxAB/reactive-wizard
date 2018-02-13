package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import java.util.function.Function;

/**
 * Generic number deserializer, not allowing null.
 */
public class NumberNotNullDeserializer<T> extends NumberDeserializer<T> {

    public NumberNotNullDeserializer(Function<String, T> numberParser, String errorCode) {
        super(numberParser, errorCode);
    }

    @Override
    protected T getNullValue() throws DeserializerException {
        throw new DeserializerException(errorCode);
    }
}
