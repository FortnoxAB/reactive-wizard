package se.fortnox.reactivewizard.jaxrs.params.deserializing;

public class IntegerNotNullDeserializer extends NumberNotNullDeserializer<Integer> {
    public IntegerNotNullDeserializer() {
        super(Integer::parseInt, "invalid.int");
    }
}
