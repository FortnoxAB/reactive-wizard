package se.fortnox.reactivewizard.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Represents a getter field.
 */
public class FieldGetter<I,T> implements Getter<I,T> {
    private final Function<I,T> fieldLambda;

    public static <I,T> Getter<I,T> create(Class<I> cls, Field field) {
        AccessorUtil.MemberTypeInfo memberTypeInfo = AccessorUtil.fieldTypeInfo(cls, field);
        return new FieldGetter<>(field, memberTypeInfo.getReturnType(), memberTypeInfo.getGenericReturnType());
    }

    private final Class<?> returnType;
    private final Type     genericReturnType;

    private FieldGetter(Field field, Class<T> returnType, Type genericReturnType) {
        this.returnType = returnType;
        this.genericReturnType = genericReturnType;
        field.setAccessible(true);
        this.fieldLambda = instance -> {
            try {
                return (T)field.get(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public T invoke(I instance) {
        return fieldLambda.apply(instance);
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public Type getGenericReturnType() {
        return genericReturnType;
    }

    @Override
    public Function<I,T> getterFunction() {
        return fieldLambda;
    }
}
