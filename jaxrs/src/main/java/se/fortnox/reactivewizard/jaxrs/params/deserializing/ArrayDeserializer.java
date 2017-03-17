package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import java.lang.reflect.Array;

public class ArrayDeserializer<T> implements Deserializer<T[]> {

    private final Deserializer<T> inner;
    private final Class<T> arrayType;

    public ArrayDeserializer(Deserializer<T> inner, Class<T> arrayType) {
        this.inner = inner;
        this.arrayType = arrayType;
    }

    @Override
    public T[] deserialize(String value) throws DeserializerException {
        if (value == null) {
            return null;
        }
        String[] rawValues = value.split(",");
        try {
            T[] deserialized = (T[]) Array.newInstance(arrayType, rawValues.length);
            for (int i = 0; i < deserialized.length; i++) {
                deserialized[i] = inner.deserialize(rawValues[i]);
            }
            return deserialized;
        } catch(DeserializerException e) {
            throw e;
        } catch (Exception e) {
            throw new DeserializerException("invalid.array");
        }
    }
}
