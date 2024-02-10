package se.fortnox.reactivewizard.db.query;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Calendar;
import java.sql.Date;
import java.util.List;
import java.util.function.Function;

public class PreparedStatementParameters {

    private final PreparedStatement preparedStatement;
    private int parameterIndex = 1;

    public PreparedStatementParameters(PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
    }

    public void addLocalDate(LocalDate ld) throws SQLException {
        addDate(Date.valueOf(ld));
    }

    public void addLocalTime(LocalTime lt) throws SQLException {
        addTime(Time.valueOf(lt));
    }

    public void addLocalDateTime(LocalDateTime ldt) throws SQLException {
        addTimestamp(Timestamp.valueOf(ldt));
    }

    public void addYearMonth(YearMonth ym) throws SQLException {
        addObject(ym.getYear() * 100 + ym.getMonthValue());
    }

    public void addEnum(Enum<?> en) throws SQLException {
        addObject(en.name());
    }

    public <T> void addSerializable(T value, Function<T, String> jsonSerializer) throws SQLException {
        addObject(jsonSerializer.apply(value));
    }

    public void addNull() throws SQLException {
        preparedStatement.setNull(parameterIndex++, java.sql.Types.NULL);
    }

    public void addObject(Object value) throws SQLException {
        preparedStatement.setObject(parameterIndex++, value);
    }

    /**
     * Add array parameter.
     * @param listElementType the type name
     * @param list the elements
     * @throws SQLException on error
     */
    public void addArray(String listElementType, List<?> list) throws SQLException {
        Connection connection = preparedStatement.getConnection();
        Array      dbArray    = connection.createArrayOf(listElementType, list.toArray());
        preparedStatement.setArray(parameterIndex++, dbArray);
    }

    public void addTimestamp(Timestamp timestamp, Calendar calendar) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex++, timestamp, calendar);
    }

    public void addTimestamp(Timestamp timestamp) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex++, timestamp);
    }

    public void addDate(Date sqlDate) throws SQLException {
        preparedStatement.setDate(parameterIndex++, sqlDate);
    }

    public void addTime(Time sqlTime) throws SQLException {
        preparedStatement.setTime(parameterIndex++, sqlTime);
    }

}
