package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

class CustomClassDeserializerFactory {

    private CustomClassDeserializerFactory() {
    }

    @Nullable
    public static <T> Deserializer<T> createOrNull(Class<T> cls) {
        var methods = cls.getMethods();

        var valueOfDeserializer = findStaticMethodDeserializer(methods, "valueOf", cls);
        if (valueOfDeserializer != null) {
            return valueOfDeserializer;
        }

        var fromStringDeserializer = findStaticMethodDeserializer(methods, "fromString", cls);
        if (fromStringDeserializer != null) {
            return fromStringDeserializer;
        }

        var constructorDeserializer = findConstructorDeserializer(cls);
        if (constructorDeserializer != null) {
            return constructorDeserializer;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> Deserializer<T> findStaticMethodDeserializer(Method[] methods, String methodName, Class<T> cls) {
        var method = Arrays.stream(methods)
            .filter(it -> it.getName().equals(methodName)
                && it.getParameterCount() == 1
                && it.getParameterTypes()[0].equals(String.class)
                && Modifier.isStatic(it.getModifiers())
            )
            .findFirst()
            .orElse(null);

        if (method != null) {
            return value -> {
                try {
                    if (value == null) {
                        return null;
                    }

                    return (T)method.invoke(null, value);
                } catch (Exception e) {
                    throw deserializerExceptionFor(cls);
                }
            };
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> Deserializer<T> findConstructorDeserializer(Class<T> cls) {
        var constructor = Arrays.stream(cls.getConstructors())
            .filter(it -> it.getParameterCount() == 1 && it.getParameterTypes()[0].equals(String.class))
            .findFirst()
            .orElse(null);

        if (constructor != null) {
            return value -> {
                try {
                    if (value == null) {
                        return null;
                    }

                    return (T)constructor.newInstance(value);
                } catch (Exception e) {
                    throw deserializerExceptionFor(cls);
                }
            };
        }

        return null;
    }

    private static DeserializerException deserializerExceptionFor(Class<?> cls)   {
        return new DeserializerException(String.format("invalid.%s", cls.getSimpleName()));
    }
}
