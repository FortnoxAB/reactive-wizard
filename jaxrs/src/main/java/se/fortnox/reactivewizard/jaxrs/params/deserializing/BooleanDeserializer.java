package se.fortnox.reactivewizard.jaxrs.params.deserializing;

/**
 * Deserializes booleans.
 */
public class BooleanDeserializer implements Deserializer<Boolean> {
    @Override
    public Boolean deserialize(String value) throws DeserializerException {
        return (value == null) ? null : Boolean.valueOf(value.trim());
    }
}
