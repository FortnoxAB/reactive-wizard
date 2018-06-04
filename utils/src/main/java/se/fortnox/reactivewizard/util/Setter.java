package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;

/**
 * Interface for a setter.
 */
public interface Setter<I,T> {
    void invoke(I instance, T value) throws InvocationTargetException, IllegalAccessException;

    Class<?> getParameterType();

    Type getGenericParameterType();

    BiConsumer<I, T> setterFunction();
}
