package se.fortnox.reactivewizard.jaxrs.params.deserializing;

public class LongNotNullDeserializer extends NumberNotNullDeserializer<Long> {

    public LongNotNullDeserializer() {
        super(Long::valueOf, "invalid.long");
    }
}