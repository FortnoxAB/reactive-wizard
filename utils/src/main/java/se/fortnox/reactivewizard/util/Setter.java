package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * Interface for a setter.
 */
public interface Setter<I,T> {
    void invoke(I instance, T value) throws InvocationTargetException, IllegalAccessException;

    Class<?> getParameterType();

    Type getGenericParameterType();
}
