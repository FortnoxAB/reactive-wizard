package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Represents a getter method.
 */
public class MethodGetter implements Getter {
    public static Getter create(Class<?> cls, Method method) {
        AccessorUtil.MemberTypeInfo memberTypeInfo = AccessorUtil.getterTypeInfo(cls, method);
        return new MethodGetter(method, memberTypeInfo.getReturnType(), memberTypeInfo.getGenericReturnType());
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
