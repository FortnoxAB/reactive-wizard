package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Deserializes doubles, not allowing nulls.
 */
public class DoubleNotNullDeserializer extends NumberNotNullDeserializer<Double> {
    public DoubleNotNullDeserializer() {
        super(Double::valueOf, "invalid.double");
    }
}
