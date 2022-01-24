package se.fortnox.reactivewizard.db.query;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.sql.Date;
import java.util.List;

public class PreparedStatementParameters {
    private PreparedStatement preparedStatement;
    private int               parameterIndex = 1;

    public PreparedStatementParameters(PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
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
