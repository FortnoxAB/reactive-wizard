package se.fortnox.reactivewizard.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Represents a getter field.
 */
public class FieldGetter implements Getter {
    public static Getter create(Class<?> cls, Field field) {
        AccessorUtil.MemberTypeInfo memberTypeInfo = AccessorUtil.fieldTypeInfo(cls, field);
        return new FieldGetter(field, memberTypeInfo.getReturnType(), memberTypeInfo.getGenericReturnType());
    }

    private final Field    field;
    private final Class<?> returnType;
    private final Type     genericReturnType;

    private FieldGetter(Field field, Class<?> returnType, Type genericReturnType) {
        this.field = field;
        this.returnType = returnType;
        this.genericReturnType = genericReturnType;
        field.setAccessible(true);
    }

    @Override
    public Object invoke(Object instance) throws IllegalAccessException {
        return field.get(instance);
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public Type getGenericReturnType() {
        return genericReturnType;
    }
}
