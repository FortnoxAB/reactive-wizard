package se.fortnox.reactivewizard.jaxrs.params.deserializing;

public class DoubleNotNullDeserializer extends NumberNotNullDeserializer<Double> {
    public DoubleNotNullDeserializer() {
        super(Double::valueOf, "invalid.double");
    }
}
