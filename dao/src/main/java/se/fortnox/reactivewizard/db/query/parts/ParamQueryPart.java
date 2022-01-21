package se.fortnox.reactivewizard.db.query.parts;

import se.fortnox.reactivewizard.db.query.PreparedStatementParameters;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import se.fortnox.reactivewizard.util.PropertyResolver;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class ParamQueryPart implements DynamicQueryPart {
    private static final JsonSerializerFactory JSON_SERIALIZER_FACTORY = new JsonSerializerFactory();

    protected final PropertyResolver             argResolver;
    protected final int                          argIndex;
    private final   PreparedStatementParamSetter paramSetter;
    private final   Function<Object,Object>      getter;

    public ParamQueryPart(int argIndex, Type cls) throws SQLException {
        this(argIndex, ReflectionUtil.getPropertyResolver(cls, new String[0]).get());
    }

    protected ParamQueryPart(ParamQueryPart paramQueryPart) throws SQLException {
        this(paramQueryPart.argIndex, paramQueryPart.argResolver);
    }

    protected ParamQueryPart(int argIndex, PropertyResolver argResolver) throws SQLException {
        this.argIndex = argIndex;
        this.argResolver = argResolver;
        this.getter = argResolver.getter();
        paramSetter = createParamSetter(argResolver.getPropertyGenericType());
    }

    @Override
    public void visit(StringBuilder sql, Object[] args) {
        sql.append("?");
    }

    @Override
    public void addParams(PreparedStatementParameters parameters, Object[] args) throws SQLException {
        Object val = getValue(args);
        if (val == null) {
            parameters.addNull();
        } else {
            paramSetter.call(parameters, val);
        }
    }

    protected Object getValue(Object[] args) {
        return getter.apply(args[argIndex]);
    }

    @Override
    public DynamicQueryPart subPath(String[] subPath) throws SQLException {
        Optional<PropertyResolver> propertyResolver = argResolver.subPath(subPath);
        if (!propertyResolver.isPresent()) {
            throw new RuntimeException(String.format("Properties %s cannot be found in %s",
                Arrays.toString(subPath), argResolver.getPropertyType()));
        }
        return new ParamQueryPart(argIndex, propertyResolver.get());
    }

    protected PreparedStatementParamSetter createParamSetter(Type type) throws SQLException {
        Class<?> rawType = ReflectionUtil.getRawType(type);
        if (LocalDate.class.isAssignableFrom(rawType)) {
            return (parameters, value) -> {
                java.sql.Date sqlDate = java.sql.Date.valueOf((LocalDate)value);
                parameters.addDate(sqlDate);
            };
        } else if (LocalTime.class.isAssignableFrom(rawType)) {
            return (parameters, value) -> {
                java.sql.Time sqlTime = java.sql.Time.valueOf((LocalTime)value);
                parameters.addTime(sqlTime);
            };
        } else if (LocalDateTime.class.isAssignableFrom(rawType)) {
            return (parameters, value) -> {
                java.sql.Timestamp sqlTimestamp = java.sql.Timestamp.valueOf((LocalDateTime)value);
                parameters.addTimestamp(sqlTimestamp);
            };
        } else if (YearMonth.class.isAssignableFrom(rawType)) {
            return (parameters, value) -> {
                YearMonth yearMonth = (YearMonth) value;
                parameters.addObject(yearMonth.getYear() * 100 + yearMonth.getMonthValue());
            };
        } else if (List.class.isAssignableFrom(rawType)) {
            Optional<String> listElementType = getListElementType(type);
            if (!listElementType.isPresent()) {
                Function<Object, String> jsonSerializer = JSON_SERIALIZER_FACTORY.createStringSerializer(type);
                return (parameters, value) -> parameters.addObject(jsonSerializer.apply(value));
            }
            String elementType = listElementType.get();
            return (parameters, value) -> {
                List<?> list = (List<?>)value;
                parameters.addArray(elementType, list);
            };
        } else if (rawType.isEnum()) {
            return (parameters, value) -> {
                Enum<?> enumValue = (Enum<?>)value;
                parameters.addObject(enumValue.name());
            };
        } else if (Map.class.isAssignableFrom(rawType)) {
            Function<Map, String> jsonSerializer = JSON_SERIALIZER_FACTORY.createStringSerializer(Map.class);
            return (parameters, value) -> parameters.addObject(jsonSerializer.apply((Map)value));
        } else {
            return (parameters, value) -> parameters.addObject(value);
        }
    }

    private Optional<String> getListElementType(Type type) {
        Class genericParam = ReflectionUtil.getGenericParameter(type);
        if (genericParam.equals(String.class) || genericParam.isEnum()) {
            return Optional.of("varchar");
        } else if (genericParam.equals(Long.class)) {
            return Optional.of("bigint");
        } else if (genericParam.equals(Integer.class)) {
            return Optional.of("integer");
        } else if (genericParam.equals(UUID.class)) {
            return Optional.of("uuid");
        } else {
            return Optional.empty();
        }
    }

    protected interface PreparedStatementParamSetter {
        void call(PreparedStatementParameters parameters, Object value) throws SQLException;
    }
}
