package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Represents a getter method.
 */
public class MethodGetter implements Getter {
    private final Method method;

    public MethodGetter(Method method) {
        this.method = method;
    }

    @Override
    public Object invoke(Object instance) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(instance);
    }

    @Override
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    public Type getGenericReturnType() {
        return method.getGenericReturnType();
    }
}
