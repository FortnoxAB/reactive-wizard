package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import se.fortnox.reactivewizard.util.LambdaCompiler;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Function;

class CustomClassDeserializerFactory {

    private CustomClassDeserializerFactory() {
    }

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    @Nullable
    public static <T> Deserializer<T> createOrNull(Class<T> cls) {
        var methods = cls.getMethods();

        var valueOfDeserializer = getStaticMethodDeserializerOrNull(methods, "valueOf", cls);
        if (valueOfDeserializer != null) {
            return valueOfDeserializer;
        }

        var fromStringDeserializer = getStaticMethodDeserializerOrNull(methods, "fromString", cls);
        if (fromStringDeserializer != null) {
            return fromStringDeserializer;
        }

        var constructorDeserializer = getConstructorDeserializerOrNull(cls);
        if (constructorDeserializer != null) {
            return constructorDeserializer;
        }

        return null;
    }

    @Nullable
    private static <T> Deserializer<T> getStaticMethodDeserializerOrNull(Method[] methods, String methodName, Class<T> cls) {
        var method = Arrays.stream(methods)
            .filter(it -> it.getName().equals(methodName)
                && it.getParameterCount() == 1
                && it.getParameterTypes()[0].equals(String.class)
                && Modifier.isStatic(it.getModifiers())
            )
            .findFirst()
            .orElse(null);

        if (method == null) {
            return null;
        }

        var compiledFunction = compileMethod(method, cls);

        return createDeserializerFromFunction(compiledFunction, cls);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> Deserializer<T> getConstructorDeserializerOrNull(Class<T> cls) {
        var constructor = (Constructor<T>)Arrays.stream(cls.getConstructors())
            .filter(it -> it.getParameterCount() == 1 && it.getParameterTypes()[0].equals(String.class))
            .findFirst()
            .orElse(null);

        if (constructor == null) {
            return null;
        }

        var compiledFunction = compileConstructor(constructor, cls);

        return createDeserializerFromFunction(compiledFunction, cls);
    }

    private static <T> Function<String, T> compileMethod(Method method, Class<T> cls) {
        try {
            return LambdaCompiler.compileLambdaFunction(lookup, lookup.unreflect(method));
        } catch (Throwable t) {
            throw new RuntimeException(String.format("Unable to compile '%s' method for '%s'", method.getName(), cls.getSimpleName()), t);
        }
    }

    private static <T> Function<String, T> compileConstructor(Constructor<T> constructor, Class<T> cls) {
        try {
            return LambdaCompiler.compileLambdaFunction(lookup, lookup.unreflectConstructor(constructor));
        } catch (Throwable t) {
            throw new RuntimeException(String.format("Unable to compile constructor for '%s'", cls.getSimpleName()), t);
        }
    }

    private static <T> Deserializer<T> createDeserializerFromFunction(Function<String, T> function, Class<T> cls) {
        return value -> {
            try {
                if (value == null) {
                    return null;
                }

                return function.apply(value);
            } catch (Exception e) {
                throw new DeserializerException(String.format("invalid.%s", cls.getSimpleName()));
            }
        };
    }
}
