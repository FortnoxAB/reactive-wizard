package se.fortnox.reactivewizard.db.deserializing;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ColumnDeserializerFactory {

    static Deserializer getColumnDeserializer(Class<?> columnClass, int columnType, int columnIndex) {
        if (columnClass.equals(String.class)) {
            return (resultSet) -> Optional.ofNullable(resultSet.getString(columnIndex));
        } else if (columnClass.equals(int.class)) {
            return (resultSet) -> Optional.of(resultSet.getInt(columnIndex));
        } else if (columnClass.equals(Integer.class)) {
            return (resultSet) -> mayBeNull(resultSet, resultSet.getInt(columnIndex));
        } else if (columnClass.equals(long.class)) {
            return (resultSet) -> Optional.of(resultSet.getLong(columnIndex));
        } else if (columnClass.equals(Long.class)) {
            return (resultSet) -> mayBeNull(resultSet, resultSet.getLong(columnIndex));
        } else if (columnClass.equals(boolean.class)) {
            return (resultSet) -> Optional.of(resultSet.getBoolean(columnIndex));
        } else if (columnClass.equals(Boolean.class)) {
            return (resultSet) -> mayBeNull(resultSet, resultSet.getBoolean(columnIndex));
        } else if (columnClass.isEnum()) {
            return (resultSet) -> Optional.ofNullable(getEnum(columnClass, resultSet, columnIndex));
        } else if (columnClass.equals(float.class)) {
            return (resultSet) -> Optional.of(resultSet.getFloat(columnIndex));
        } else if (columnClass.equals(Float.class)) {
            return (resultSet) -> mayBeNull(resultSet, resultSet.getFloat(columnIndex));
        } else if (columnClass.equals(double.class)) {
            return (resultSet) -> Optional.of(resultSet.getDouble(columnIndex));
        } else if (columnClass.equals(Double.class)) {
            return (resultSet) -> mayBeNull(resultSet, resultSet.getDouble(columnIndex));
        } else if (columnClass.equals(BigDecimal.class)) {
            return (resultSet) -> mayBeNull(resultSet, resultSet.getBigDecimal(columnIndex));
        } else if (columnClass.equals(UUID.class)) {
            return (resultSet) -> mayBeNull(resultSet, resultSet.getObject(columnIndex));
        } else if (columnClass.equals(LocalDate.class)) {
            return (resultSet) -> Optional.ofNullable(getLocalDate(resultSet, columnIndex));
        } else if (columnClass.equals(LocalTime.class)) {
            return (resultSet) -> Optional.ofNullable(getLocalTime(resultSet, columnIndex));
        } else if (columnClass.equals(LocalDateTime.class)) {
            return (resultSet) -> Optional.ofNullable(getLocalDateTime(resultSet, columnIndex));
        } else if (columnClass.equals(YearMonth.class)) {
            return (resultSet) -> mayBeNull(resultSet, resultSet.getInt(columnIndex)).map(ColumnDeserializerFactory::toYearMonth);
        } else if (columnClass.isArray()) {
            if (columnClass.equals(byte[].class)) {
                return (resultSet) -> Optional.ofNullable(resultSet.getBytes(columnIndex));
            }
            return (resultSet) -> Optional.ofNullable(getArray(resultSet, columnIndex));
        } else if (columnClass.isAssignableFrom(List.class) && columnType == Types.ARRAY) {
            return (resultSet) -> Optional.ofNullable(getList(resultSet, columnIndex));
        }
        return null;
    }

    private static YearMonth toYearMonth(Integer integer) {
        return YearMonth.of(integer/100, integer%100);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object getEnum(Class columnClass, ResultSet resultSet, int columnIndex)
        throws SQLException {
        String string = resultSet.getString(columnIndex);
        if (string == null) {
            return null;
        }
        return Enum.valueOf(columnClass, string);
    }

    private static <T> Optional<T> mayBeNull(ResultSet resultSet, T value)
        throws SQLException {
        if (resultSet.wasNull()) {
            value = null;
        }
        return Optional.ofNullable(value);
    }

    private static LocalDate getLocalDate(ResultSet resultSet, int columnIndex) throws SQLException {
        java.sql.Date date = resultSet.getDate(columnIndex);
        if (date != null) {
            return date.toLocalDate();
        }
        return null;
    }

    private static LocalTime getLocalTime(ResultSet resultSet, int columnIndex) throws SQLException {
        java.sql.Time time = resultSet.getTime(columnIndex);
        if (time != null) {
            return time.toLocalTime();
        }
        return null;
    }

    private static LocalDateTime getLocalDateTime(ResultSet resultSet, int columnIndex) throws SQLException {
        java.sql.Timestamp timestamp = resultSet.getTimestamp(columnIndex);
        if (timestamp != null) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    private static Object getArray(ResultSet resultSet, int columnIndex) throws SQLException {
        Array array = resultSet.getArray(columnIndex);
        if (array != null) {
            return array.getArray();
        }
        return null;
    }

    private static Object getList(ResultSet resultSet, int columnIndex) throws SQLException {
        Array array = resultSet.getArray(columnIndex);
        if (array != null) {
            return Arrays.asList((Object[])array.getArray());
        }
        return null;
    }
}
