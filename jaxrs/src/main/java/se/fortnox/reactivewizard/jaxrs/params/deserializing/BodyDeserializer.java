package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Parses an object out of a byte[]
 */
public interface BodyDeserializer<T> {
    T deserialize(byte[] value) throws DeserializerException;
}
