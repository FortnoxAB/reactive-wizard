package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Represents a getter method.
 */
public class MethodGetter implements Getter {
    public static Getter create(Class<?> cls, Method method) {
        Map<String, Class<?>> genericTypenameToType = AccessorUtil.typesByGenericName(cls, method);
        Class<?> returnType = method.getDeclaringClass().equals(cls) ?
            method.getReturnType() :
            genericTypenameToType.get(method.getGenericReturnType().getTypeName());
        Type genericReturnType = method.getDeclaringClass().equals(cls) ?
            method.getGenericReturnType() :
            genericTypenameToType.get(method.getGenericReturnType().getTypeName());
        return new MethodGetter(method, returnType, genericReturnType);
    }

    private final Method   method;
    private final Class<?> returnType;
    private final Type     genericReturnType;

    private MethodGetter(Method method, Class<?> returnType, Type genericReturnType) {
        this.returnType = returnType;
        this.method = method;
        this.genericReturnType = genericReturnType;
    }

    @Override
    public Object invoke(Object instance) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(instance);
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public Type getGenericReturnType() {
        return genericReturnType;
    }
}
