package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Deserializes strings such as "2007-12-03T10:15:30" using
 * {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
 */
public class LocalDateTimeDeserializer implements Deserializer<LocalDateTime> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDateTimeDeserializer.class);

    @Override
    public LocalDateTime deserialize(String value) throws DeserializerException {
        if (value == null || value.length() == 0) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            LOG.warn("Unable to parse " + value + " as LocalDateTime", e);
            throw new DeserializerException("invalid.localdatetime");
        }

    }
}
