package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class LocalTimeDeserializer implements Deserializer<LocalTime> {

    private static final Logger LOG = LoggerFactory
        .getLogger(LocalTimeDeserializer.class);

    @Override
    public LocalTime deserialize(String value) throws DeserializerException {
        if (value == null || value.length() == 0) {
            return null;
        }

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            LOG.warn("Unable to parse " + value + " as LocalTime", e);
            throw new DeserializerException("invalid.localtime");
        }

    }
}
