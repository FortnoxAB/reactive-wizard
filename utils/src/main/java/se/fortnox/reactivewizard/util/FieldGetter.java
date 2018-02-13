package se.fortnox.reactivewizard.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * Represents a getter field.
 */
public class FieldGetter implements Getter {
    private final Field field;

    public FieldGetter(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    @Override
    public Object invoke(Object instance) throws InvocationTargetException, IllegalAccessException {
        return field.get(instance);
    }

    @Override
    public Class<?> getReturnType() {
        return field.getType();
    }

    @Override
    public Type getGenericReturnType() {
        return field.getGenericType();
    }
}
