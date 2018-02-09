package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Deserializes longs.
 */
public class LongDeserializer extends NumberDeserializer<Long> {

    public LongDeserializer() {
        super(Long::valueOf, "invalid.long");
    }
}
