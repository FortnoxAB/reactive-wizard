package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Extending the jackson impl with the format yyyy-MM-dd HH:mm:ss
 *
 * @author jonashall
 *
 */
@SuppressWarnings("serial")
public class RWDateFormat extends StdDateFormat {

	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone
			.getTimeZone("Europe/Stockholm");

	public RWDateFormat() {
		super(DEFAULT_TIME_ZONE, Locale.getDefault(), true);
	}

	@Override
	public Date parse(String dateStr) throws ParseException {
		try {
			return super.parse(dateStr);
		} catch(ParseException e) {
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			fmt.setTimeZone(DEFAULT_TIME_ZONE);
			return fmt.parse(dateStr);
		}
	}

	@Override
	public StdDateFormat clone() {
		return new RWDateFormat();
	}
}
