package se.fortnox.reactivewizard.dates;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public class Dates {
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Stockholm");

    static {
        TimeZone.setDefault(DEFAULT_TIME_ZONE);
    }

    public static Date truncateDate(Date date) {
        if (date == null) {
            return null;
        }

        Calendar cal = getCalendar(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    public static Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(DEFAULT_TIME_ZONE);
        return calendar;
    }

    public static Calendar getCalendar(Date date) {
        Calendar calendar = getCalendar();
        calendar.setTime(date);
        return calendar;
    }

    public static Calendar getCalendar(String date) {
        return getCalendar(parseDate(date));
    }

    public static Date parseDate(String date) {
        try {
            return dateFormat().parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isDate(String date) {
        try {
            parseDate(date);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    protected static DateFormat dateFormat() {
        return dateFormat("yyyy-MM-dd");
    }

    public static DateFormat dateFormat(String fmtStr) {
        SimpleDateFormat format = new SimpleDateFormat(fmtStr);
        format.setTimeZone(DEFAULT_TIME_ZONE);
        return format;
    }

    public static String format(Date date) {
        return dateFormat().format(date);
    }

    public static String format(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String format(Date date, String format) {
        return dateFormat(format).format(date);
    }

    public static String format(LocalDate date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return date.format(formatter);
    }

    public static Date midnight() {
        return truncateDate(new Date());
    }

    public static Date addDays(Date date, int days) {
        Calendar cal = getCalendar(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    public static Date subtractDays(Date date, int days) {
        return addDays(date, -days);
    }

    public static boolean isSameDay(Date firstDate, Date secondDate) {
        return Objects.equals(Dates.truncateDate(firstDate), Dates.truncateDate(secondDate));
    }

    public static boolean isSameDay(LocalDate firstDate, LocalDate secondDate) {
        return Objects.equals(firstDate, secondDate);
    }

    public static boolean dayIsBefore(Date firstDate, Date secondDate) {
        if (firstDate == null || secondDate == null) {
            return false;
        }
        return Dates.truncateDate(firstDate).before(Dates.truncateDate(secondDate));
    }

    public static boolean dayIsBefore(LocalDate firstDate, LocalDate secondDate) {
        if (firstDate == null || secondDate == null) {
            return false;
        }
        return firstDate.isBefore(secondDate);
    }

    public static boolean dayIsAfter(Date firstDate, Date secondDate) {
        if (firstDate == null || secondDate == null) {
            return false;
        }
        return Dates.truncateDate(firstDate).after(Dates.truncateDate(secondDate));
    }

    public static boolean dayIsAfter(LocalDate firstDate, LocalDate secondDate) {
        if (firstDate == null || secondDate == null) {
            return false;
        }
        return firstDate.isAfter(secondDate);
    }

    public static LocalDate localDateFromDate(Date date) {
        return LocalDate.parse(Dates.format(Dates.truncateDate(date)));
    }

    public static Date dateFromLocalDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Dates.parseDate(date.toString());
    }
}
