package se.fortnox.reactivewizard.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;

/**
 * Represents a setter field.
 */
public class FieldSetter<I,T> implements Setter<I,T> {
    public static <I,T> Setter<I,T> create(Class<I> cls, Field field) {
        AccessorUtil.MemberTypeInfo memberTypeInfo = AccessorUtil.fieldTypeInfo(cls, field);
        return new FieldSetter<>(field, memberTypeInfo.getReturnType(), memberTypeInfo.getGenericReturnType());
    }

    private final Field           field;
    private final BiConsumer<I,T> setter;
    private final Class<?>        parameterType;
    private final Type            genericParameterType;

    private FieldSetter(Field field, Class<T> parameterType, Type genericParameterType) {
        this.field = field;
        this.parameterType = parameterType;
        this.genericParameterType = genericParameterType;
        field.setAccessible(true);
        setter = (instance,value) -> {
            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public void invoke(I instance, T value) throws IllegalAccessException {
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

    @Override
    public BiConsumer<I,T> setterFunction() {
        return setter;
    }
}
