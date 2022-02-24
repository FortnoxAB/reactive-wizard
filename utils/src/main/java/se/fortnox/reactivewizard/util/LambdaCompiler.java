package se.fortnox.reactivewizard.util;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LambdaCompiler {
    static boolean useLambdas = "true".equals(System.getProperty("useLambdas", "true"));

    static <T> Supplier<T> compileLambdaSupplier(MethodHandles.Lookup lookup, MethodHandle methodHandle) throws Throwable {
        if (!useLambdas) {
            return () -> {
                try {
                    return (T) methodHandle.invoke();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        }
        CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "get",
            MethodType.methodType(Supplier.class),
            MethodType.methodType(Object.class),
            methodHandle,
            methodHandle.type()
        );
        return (Supplier<T>)callSite.getTarget().invoke();
    }

    static <I,T> BiConsumer<I,T> compileLambdaBiConsumer(MethodHandles.Lookup lookup, MethodHandle methodHandle) throws Throwable {
        if (!useLambdas) {
            return (instance, arg) -> {
                try {
                    methodHandle.bindTo(instance).invokeWithArguments(arg);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        }
        CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "accept",
            MethodType.methodType(BiConsumer.class),
            MethodType.methodType(void.class, Object.class, Object.class),
            methodHandle,
            methodHandle.type().wrap().changeReturnType(void.class)
        );
        return (BiConsumer<I,T>) callSite.getTarget().invoke();
    }

    static <I,T> Function<I, T> compileLambdaFunction(MethodHandles.Lookup lookup, MethodHandle methodHandle) throws Throwable {
        if (!useLambdas) {
            return (instance) -> {
                try {
                    return (T)methodHandle.bindTo(instance).invoke();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        }
        CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "apply",
            MethodType.methodType(Function.class),
            MethodType.methodType(Object.class, Object.class),
            methodHandle,
            methodHandle.type()
        );
        return (Function<I,T>) callSite.getTarget().invoke();
    }
}
