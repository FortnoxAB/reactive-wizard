package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import jakarta.inject.Provider;

import java.text.DateFormat;
import java.util.Date;

/**
 * Deserializes dates.
 */
public class DateDeserializer implements Deserializer<Date> {
    private Provider<DateFormat> dateFormatProvider;

    public DateDeserializer(Provider<DateFormat> dateFormatProvider) {
        this.dateFormatProvider = dateFormatProvider;
    }

    @Override
    public Date deserialize(String value) throws DeserializerException {
        if (value == null) {
            return null;
        }
        try {
            return new Date(Long.parseLong(value));
        } catch (NumberFormatException e) {
            try {
                return dateFormatProvider.get().parse(value);
            } catch (Exception parseException) {
                throw new DeserializerException("invalid.date");
            }
        }
    }
}
