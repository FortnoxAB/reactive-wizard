package se.fortnox.reactivewizard.util;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;

/**
 * Represents a setter method.
 */
public class MethodSetter<I,T> implements Setter<I,T> {

    public static <I,T> Setter<I,T> create(Class<I> cls, Method method) {
        AccessorUtil.MemberTypeInfo memberTypeInfo = AccessorUtil.setterTypeInfo(cls, method);
        return new MethodSetter<>(method, memberTypeInfo.getReturnType(), memberTypeInfo.getGenericReturnType());
    }

    private final BiConsumer<I, T> setterLambda;
    private final Class<?> parameterType;
    private final Type     genericParameterType;

    private MethodSetter(Method method, Class<T> parameterType, Type genericParameterType) {
        this.parameterType = parameterType;
        this.genericParameterType = genericParameterType;

        MethodHandles.Lookup lookup = ReflectionUtil.lookupFor(method.getDeclaringClass(), method);

        try {
            MethodHandle methodHandle = lookup.unreflect(method);
            setterLambda = LambdaCompiler.compileLambdaBiConsumer(lookup, methodHandle);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void invoke(I instance, T value) {
        setterLambda.accept(instance, value);
    }

    @Override
    public Class<?> getParameterType() {
        return parameterType;
    }

    @Override
    public Type getGenericParameterType() {
        return genericParameterType;
    }

    @Override
    public BiConsumer<I, T> setterFunction() {
        return setterLambda;
    }
}
