package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import java.util.UUID;

public class UUIDDeserializer implements Deserializer<UUID> {
    @Override
    public UUID deserialize(String value) throws DeserializerException {
        return value == null || value.equals("") ? null : UUID.fromString(value);
    }
}
