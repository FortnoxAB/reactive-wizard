package se.fortnox.reactivewizard.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;

import static se.fortnox.reactivewizard.util.ReflectionUtil.lambdaForFunction;

/**
 * Represents a getter method.
 */
public class MethodGetter<I,T> implements Getter<I,T> {
    private final Function<I, T> getterLambda;

    public static Getter create(Class<?> cls, Method method) {
        AccessorUtil.MemberTypeInfo memberTypeInfo = AccessorUtil.getterTypeInfo(cls, method);
        return new MethodGetter(method, memberTypeInfo.getReturnType(), memberTypeInfo.getGenericReturnType());
    }

    private final Class<?> returnType;
    private final Type     genericReturnType;

    private MethodGetter(Method method, Class<T> returnType, Type genericReturnType) {
        this.returnType = returnType;
        this.genericReturnType = genericReturnType;

        MethodHandles.Lookup lookup = ReflectionUtil.lookupFor(method.getDeclaringClass(), method);

        try {
            MethodHandle methodHandle = lookup.unreflect(method);
            getterLambda = lambdaForFunction(lookup, methodHandle);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }

    }

    @Override
    public T invoke(I instance) {
        return getterLambda.apply(instance);
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public Type getGenericReturnType() {
        return genericReturnType;
    }

    @Override
    public Function<I, T> getterFunction() {
        return getterLambda;
    }
}
