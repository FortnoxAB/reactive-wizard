package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

/**
 * Deserializes date and time as LocalDateTime.
 *
 * Uses the {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE_TIME} format.
 */
public class LocalDateTimeDeserializer implements Deserializer<LocalDateTime> {
    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Stockholm");
    private static final Logger   LOG               = LoggerFactory.getLogger(LocalDateTimeDeserializer.class);

    @Override
    public LocalDateTime deserialize(String value) throws DeserializerException {
        if (value == null || value.length() == 0) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            LOG.warn("Unable to parse " + value + " as LocalDateTime", e);
        }

        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(value)), DEFAULT_TIME_ZONE.toZoneId());
        } catch (NumberFormatException | DateTimeException e) {
            throw new DeserializerException("invalid.localdatetime");
        }
    }
}
