package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Deserializes integers.
 */
public class IntegerDeserializer extends NumberDeserializer<Integer> {
    public IntegerDeserializer() {
        super(Integer::valueOf, "invalid.int");
    }
}
