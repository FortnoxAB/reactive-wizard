package se.fortnox.reactivewizard.db.query.parts;

import se.fortnox.reactivewizard.db.query.ParamSetter;
import se.fortnox.reactivewizard.db.query.ParamSetterFactory;
import se.fortnox.reactivewizard.util.PropertyResolver;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class ParamQueryPart implements DynamicQueryPart {

    protected final PropertyResolver        argResolver;
    protected final int                     argIndex;
    private final   ParamSetterFactory      paramSetterFactory;
    private final   Function<Object,Object> getter;

    public ParamQueryPart(int argIndex, Type cls) {
        this(argIndex, ReflectionUtil.getPropertyResolver(cls, new String[0]).get());
    }

    protected ParamQueryPart(ParamQueryPart paramQueryPart) {
        this(paramQueryPart.argIndex, paramQueryPart.argResolver);
    }

    protected ParamQueryPart(int argIndex, PropertyResolver argResolver) {
        this.argIndex = argIndex;
        this.argResolver = argResolver;
        this.getter = argResolver.getter();
        paramSetterFactory = createParamSetterFactory(argResolver.getPropertyGenericType());
    }

    @Override
    public void visit(StringBuilder sql, Object[] args) {
        sql.append("?");
    }

    @Override
    public void addParamSetter(List<ParamSetter> paramSetters, Object[] args) {
        var val = getValue(args);
        if (val == null) {
            paramSetters.add((parameters) -> parameters.addNull());
        } else {
            paramSetters.add(paramSetterFactory.create(val));
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

    protected ParamSetterFactory createParamSetterFactory(Type type) {
        return ParamSetterFactory.forType(type, () -> getListElementType(type));
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
}
