package se.fortnox.reactivewizard.jaxrs.params.deserializing;

public class IntegerDeserializer extends NumberDeserializer<Integer> {
    public IntegerDeserializer() {
        super(Integer::valueOf, "invalid.int");
    }
}
