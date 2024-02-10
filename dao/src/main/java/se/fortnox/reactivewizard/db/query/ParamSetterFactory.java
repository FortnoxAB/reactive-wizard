package se.fortnox.reactivewizard.db.query;

import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface ParamSetterFactory {
    ParamSetter create(Object value);

    JsonSerializerFactory JSON_SERIALIZER_FACTORY = new JsonSerializerFactory();

    static ParamSetterFactory forType(Type type, Supplier<Optional<String>> listElementTypeSelector) {
        Class<?> rawType = ReflectionUtil.getRawType(type);
        if (LocalDate.class.isAssignableFrom(rawType)) {
            return (value) -> (parameters) -> parameters.addLocalDate((LocalDate)value);
        } else if (LocalTime.class.isAssignableFrom(rawType)) {
            return (value) -> (parameters) -> parameters.addLocalTime((LocalTime)value);
        } else if (LocalDateTime.class.isAssignableFrom(rawType)) {
            return (value) -> (parameters) -> parameters.addLocalDateTime((LocalDateTime)value);
        } else if (YearMonth.class.isAssignableFrom(rawType)) {
            return (value) -> (parameters) -> parameters.addYearMonth((YearMonth) value);
        } else if (List.class.isAssignableFrom(rawType)) {
            Optional<String> listElementType = listElementTypeSelector.get();
            if (!listElementType.isPresent()) {
                var jsonSerializer = JSON_SERIALIZER_FACTORY.createStringSerializer(type);
                return (value) -> (parameters) -> parameters.addSerializable(value, jsonSerializer);
            }
            String elementType = listElementType.get();
            return (value) -> (parameters) -> parameters.addArray(elementType, (List<?>)value);
        } else if (rawType.isEnum()) {
            return (value) -> (parameters) -> parameters.addEnum((Enum<?>)value);
        } else if (Map.class.isAssignableFrom(rawType)) {
            var jsonSerializer = JSON_SERIALIZER_FACTORY.createStringSerializer(Map.class);
            return (value) -> (parameters) -> parameters.addSerializable((Map)value, jsonSerializer);
        } else {
            return (value) -> (parameters) -> parameters.addObject(value);
        }
    }
}
