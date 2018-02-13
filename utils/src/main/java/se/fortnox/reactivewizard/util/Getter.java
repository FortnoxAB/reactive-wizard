package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * Interface for a getter.
 */
public interface Getter {
    Object invoke(Object instance) throws InvocationTargetException, IllegalAccessException;

    Class<?> getReturnType();

    Type getGenericReturnType();
}

