package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * Interface for a setter.
 */
public interface Setter {
    void invoke(Object instance, Object value) throws InvocationTargetException, IllegalAccessException;

    Class<?> getParameterType();

    Type getGenericParameterType();
}
