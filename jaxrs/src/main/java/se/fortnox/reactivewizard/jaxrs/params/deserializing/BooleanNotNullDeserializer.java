package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Deserializes booleans, not allowing nulls.
 */
public class BooleanNotNullDeserializer implements Deserializer<Boolean> {
    @Override
    public Boolean deserialize(String value) throws DeserializerException {
        if (value == null) {
            throw new DeserializerException("invalid.boolean");
        }
        return Boolean.valueOf(value.trim());
    }
}
