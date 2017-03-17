package se.fortnox.reactivewizard.jaxrs.params.deserializing;

public class BooleanNotNullDeserializer implements Deserializer<Boolean> {
    @Override
    public Boolean deserialize(String value) throws DeserializerException {
        if (value == null) {
            throw new DeserializerException("invalid.boolean");
        }
        return Boolean.valueOf(value.trim());
    }
}
