package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Deserializes integers, not allowing null.
 */
public class IntegerNotNullDeserializer extends NumberNotNullDeserializer<Integer> {
    public IntegerNotNullDeserializer() {
        super(Integer::parseInt, "invalid.int");
    }
}
