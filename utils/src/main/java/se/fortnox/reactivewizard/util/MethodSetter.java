package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Represents a setter method.
 */
public class MethodSetter implements Setter {
    public static Setter create(Class<?> cls, Method method) {
        AccessorUtil.MemberTypeInfo memberTypeInfo = AccessorUtil.setterTypeInfo(cls, method);
        return new MethodSetter(method, memberTypeInfo.getReturnType(), memberTypeInfo.getGenericReturnType());
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
