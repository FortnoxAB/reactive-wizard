package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import java.util.UUID;

public class UUIDDeserializer implements Deserializer<UUID> {
    @Override
    public UUID deserialize(String value) throws DeserializerException {
        try {
            return value == null || value.equals("") ? null : UUID.fromString(value);
        } catch (Exception e) {
            throw new DeserializerException("invalid.uuid");
        }
    }
}
