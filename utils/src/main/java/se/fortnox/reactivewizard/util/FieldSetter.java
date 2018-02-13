package se.fortnox.reactivewizard.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * Represents a setter field.
 */
public class FieldSetter implements Setter {
    private final Field field;

    public FieldSetter(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    @Override
    public void invoke(Object instance, Object value) throws InvocationTargetException, IllegalAccessException {
        field.set(instance, value);
    }

    @Override
    public Class<?> getParameterType() {
        return field.getType();
    }

    @Override
    public Type getGenericParameterType() {
        return field.getGenericType();
    }
}
