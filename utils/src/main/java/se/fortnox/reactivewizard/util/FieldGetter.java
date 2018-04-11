package se.fortnox.reactivewizard.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Represents a getter field.
 */
public class FieldGetter implements Getter {
    public static Getter create(Class<?> cls, Field field) {
        Map<String, Class<?>> genericTypenameToType = AccessorUtil.typesByGenericName(cls, field);
        Class<?> returnType = field.getDeclaringClass().equals(cls) ?
            field.getType() :
            genericTypenameToType.get(field.getGenericType().getTypeName());
        Type genericReturnType = field.getDeclaringClass().equals(cls) ?
            field.getGenericType() :
            genericTypenameToType.get(field.getGenericType().getTypeName());
        return new FieldGetter(field, returnType, genericReturnType);
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
