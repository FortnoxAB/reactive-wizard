package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

public interface Getter {
    Object invoke(Object instance) throws InvocationTargetException, IllegalAccessException;

    Class<?> getReturnType();

    Type getGenericReturnType();
}

