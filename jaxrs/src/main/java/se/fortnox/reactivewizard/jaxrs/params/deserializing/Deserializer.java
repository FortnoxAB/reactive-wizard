package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Parses an object out of a string
 */
public interface Deserializer<T> {
    T deserialize(String value) throws DeserializerException;
}
