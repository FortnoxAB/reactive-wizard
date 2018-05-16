package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Interface for a getter.
 */
public interface Getter<I,T> {
    T invoke(I instance) throws InvocationTargetException, IllegalAccessException;

    Class<?> getReturnType();

    Type getGenericReturnType();

    Function<I,T> getterFunction();
}

