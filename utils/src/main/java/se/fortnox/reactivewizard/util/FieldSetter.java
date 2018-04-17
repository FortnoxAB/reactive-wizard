package se.fortnox.reactivewizard.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Represents a setter field.
 */
public class FieldSetter implements Setter {
    public static Setter create(Class<?> cls, Field field) {
        AccessorUtil.MemberTypeInfo memberTypeInfo = AccessorUtil.fieldTypeInfo(cls, field);
        return new FieldSetter(field, memberTypeInfo.getReturnType(), memberTypeInfo.getGenericReturnType());
    }

    private final Field    field;
    private final Class<?> parameterType;
    private final Type     genericParameterType;

    private FieldSetter(Field field, Class<?> parameterType, Type genericParameterType) {
        this.field = field;
        this.parameterType = parameterType;
        this.genericParameterType = genericParameterType;
        field.setAccessible(true);
    }

    @Override
    public void invoke(Object instance, Object value) throws IllegalAccessException {
        field.set(instance, value);
    }

    @Override
    public Class<?> getParameterType() {
        return parameterType;
    }

    @Override
    public Type getGenericParameterType() {
        return genericParameterType;
    }
}
