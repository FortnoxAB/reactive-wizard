package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Deserializes longs, not allowing null.
 */
public class LongNotNullDeserializer extends NumberNotNullDeserializer<Long> {

    public LongNotNullDeserializer() {
        super(Long::valueOf, "invalid.long");
    }
}
