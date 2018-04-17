package se.fortnox.reactivewizard.dates;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Extending the jackson impl with the format yyyy-MM-dd HH:mm:ss.
 */
@SuppressWarnings("serial")
public class DateFormat extends StdDateFormat {

    public DateFormat() {
        super(Dates.DEFAULT_TIME_ZONE, Locale.getDefault(), true);
    }

    @Override
    public Date parse(String dateStr) throws ParseException {
        try {
            return super.parse(dateStr);
        } catch (ParseException e) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format.setTimeZone(Dates.DEFAULT_TIME_ZONE);
            return format.parse(dateStr);
        }
    }

    @Override
    public StdDateFormat clone() {
        return new DateFormat();
    }
}
