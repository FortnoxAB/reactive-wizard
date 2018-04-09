package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Represents a setter method.
 */
public class MethodSetter implements Setter {
    public static Setter create(Class<?> cls, Method method) {
        Map<String, Class<?>> genericTypenameToType = Getter.typesByGenericName(cls, method);
        Class<?> returnType = method.getDeclaringClass().equals(cls) ?
            method.getParameterTypes()[0] :
            genericTypenameToType.get(method.getGenericParameterTypes()[0].getTypeName());
        Type genericReturnType = method.getDeclaringClass().equals(cls) ?
            method.getGenericParameterTypes()[0] :
            genericTypenameToType.get(method.getGenericParameterTypes()[0].getTypeName());
        return new MethodSetter(method, returnType, genericReturnType);
    }

    private final Method   method;
    private final Class<?> parameterType;
    private final Type     genericParameterType;

    private MethodSetter(Method method, Class<?> parameterType, Type genericParameterType) {
        this.method = method;
        this.parameterType = parameterType;
        this.genericParameterType = genericParameterType;
    }

    @Override
    public void invoke(Object instance, Object value) throws InvocationTargetException, IllegalAccessException {
        method.invoke(instance, value);
    }

    @Override
    public Class<?> getParameterType() {
        return parameterType;
    }

    @Override
    public Type getGenericParameterType() {
        return genericParameterType;
    }
}
