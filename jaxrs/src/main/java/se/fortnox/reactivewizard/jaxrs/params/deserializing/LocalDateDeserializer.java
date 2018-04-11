package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

/**
 * Deserializes dates as LocalDate.
 */
public class LocalDateDeserializer implements Deserializer<LocalDate> {
    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Stockholm");
    private static final Logger   LOG               = LoggerFactory.getLogger(LocalDateDeserializer.class);

    @Override
    public LocalDate deserialize(String value) throws DeserializerException {
        if (value == null || value.length() == 0) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            LOG.warn("Unable to parse " + value + " as LocalDate", e);
        }

        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(value)),
                DEFAULT_TIME_ZONE.toZoneId()).toLocalDate();
        } catch (NumberFormatException | DateTimeException e) {
            throw new DeserializerException("invalid.localdate");
        }
    }
}
