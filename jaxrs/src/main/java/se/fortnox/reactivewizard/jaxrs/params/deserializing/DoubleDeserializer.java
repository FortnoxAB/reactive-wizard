package se.fortnox.reactivewizard.jaxrs.params.deserializing;

public class DoubleDeserializer extends NumberDeserializer<Double> {

    public DoubleDeserializer() {
        super(Double::valueOf, "invalid.double");
    }
}
