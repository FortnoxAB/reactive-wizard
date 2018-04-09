package se.fortnox.reactivewizard.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Represents a setter field.
 */
public class FieldSetter implements Setter {
    public static Setter create(Class<?> cls, Field field) {
        Map<String, Class<?>> genericTypenameToType = Getter.typesByGenericName(cls, field);
        Class<?> returnType = field.getDeclaringClass().equals(cls) ?
            field.getType() :
            genericTypenameToType.get(field.getGenericType().getTypeName());
        Type genericReturnType = field.getDeclaringClass().equals(cls) ?
            field.getGenericType() :
            genericTypenameToType.get(field.getGenericType().getTypeName());
        return new FieldSetter(field, returnType, genericReturnType);
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
