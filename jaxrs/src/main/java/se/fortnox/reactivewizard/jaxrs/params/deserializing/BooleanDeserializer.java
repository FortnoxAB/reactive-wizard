package se.fortnox.reactivewizard.jaxrs.params.deserializing;

public class BooleanDeserializer implements Deserializer<Boolean> {
    @Override
    public Boolean deserialize(String value) throws DeserializerException {
        return (value == null) ? null : Boolean.valueOf(value.trim());
    }
}
