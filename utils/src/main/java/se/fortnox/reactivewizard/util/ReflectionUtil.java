package se.fortnox.reactivewizard.util;

import rx.Observable;
import rx.Single;
import se.fortnox.reactivewizard.util.rx.PropertyResolver;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class ReflectionUtil {
    public static Type getTypeOfObservable(Method method) {
        Type type = method.getGenericReturnType();
        if (!(type instanceof ParameterizedType)) {
            method = getInterfaceMethod(method);
            type = method.getGenericReturnType();
        }
        ParameterizedType parameterizedType = (ParameterizedType)type;
        Class<?>          rawClass          = (Class<?>)parameterizedType.getRawType();
        if (!(rawClass.equals(Observable.class)) && !(rawClass.equals(Single.class))) {
            throw new RuntimeException(type + " is not an Observable or Single");
        }
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        return actualTypeArguments[0];
    }

    public static Class<?> getGenericParameter(Type type) {
        if (!(type instanceof ParameterizedType)) {
            throw new RuntimeException("The sent in type " + type + " is not a ParameterizedType");
        }
        ParameterizedType pt                  = (ParameterizedType)type;
        Type[]            actualTypeArguments = pt.getActualTypeArguments();
        if (actualTypeArguments.length != 1) {
            throw new RuntimeException("The sent in type " + type + " should have exactly one type argument, but had " + actualTypeArguments);
        }
        return (Class<?>)actualTypeArguments[0];
    }

    private static Method getInterfaceMethod(Method method) {
        for (Class iface : method.getDeclaringClass().getInterfaces()) {
            for (Method candidate : iface.getDeclaredMethods()) {
                if (methodsEquals(method, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean methodsEquals(Method method, Method candidateMethod) {
        return candidateMethod.getName().equals(method.getName()) && Arrays.equals(candidateMethod.getParameterTypes(), method.getParameterTypes());
    }

    /**
     * Extracts a named property from an object using reflection.
     *
     * @param object Target object
     * @param property Property name to extract
     * @return Value of the property
     */
    public static Object getValue(Object object, String property) {
        Class<? extends Object> cls = object.getClass();
        property = property.substring(0, 1).toUpperCase() + property.substring(1);
        try {
            try {
                return cls.getMethod("get" + property, new Class[0]).invoke(object);
            } catch (NoSuchMethodException e) {
                return cls.getMethod("is" + property, new Class[0]).invoke(object);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Method getOverriddenMethod(Method method) {
        Optional<Method> found;
        for (Class<?> iface : method.getDeclaringClass().getInterfaces()) {
            found = findMethodInClass(method, iface);
            if (found.isPresent()) {
                return found.get();
            }
        }
        found = findMethodInClass(method, method.getDeclaringClass());
        return found.orElse(null);
    }

    public static Optional<Method> findMethodInClass(Method method, Class<?> cls) {
        for (Method candidate : cls.getDeclaredMethods()) {
            if (methodsEquals(method, candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
        T annotation = method.getAnnotation(annotationClass);

        if (annotation == null) {
            Method overriddenMethod = getOverriddenMethod(method);
            if (overriddenMethod != null) {
                annotation = overriddenMethod.getAnnotation(annotationClass);
            }
        }

        return annotation;
    }

    public static List<Annotation> getAnnotations(Method method) {
        List<Annotation> result     = new LinkedList<>(asList(method.getAnnotations()));
        Method           overridden = getOverriddenMethod(method);
        if (overridden != null) {
            result.addAll(asList(overridden.getAnnotations()));
        }
        return result;
    }

    public static List<List<Annotation>> getParameterAnnotations(Method method) {
        List<List<Annotation>> result               = new LinkedList<List<Annotation>>();
        Annotation[][]         annotations          = method.getParameterAnnotations();
        Method                 overridden           = getOverriddenMethod(method);
        Annotation[][]         inheritedAnnotations = null;
        if (overridden != null) {
            inheritedAnnotations = overridden.getParameterAnnotations();
        }
        for (int i = 0; i < annotations.length; i++) {
            List<Annotation> merged = new LinkedList<Annotation>();
            merged.addAll(asList(annotations[i]));
            if (inheritedAnnotations != null) {
                merged.addAll(asList(inheritedAnnotations[i]));
            }
            result.add(merged);
        }

        return result;
    }

    public static List<Annotation> getParameterAnnotations(Parameter parameter) {
        List<Annotation> annotations      = new ArrayList<>(asList(parameter.getAnnotations()));
        Method           method           = (Method)parameter.getDeclaringExecutable();
        Method           overriddenMethod = getOverriddenMethod(method);
        if (overriddenMethod != null) {
            for (Parameter overriddenParameter : overriddenMethod.getParameters()) {
                if (overriddenParameter.getName().equals(parameter.getName())) {
                    annotations.addAll(asList(overriddenParameter.getAnnotations()));
                    return annotations;
                }
            }
        }
        return annotations;
    }

    public static Class<?> getRawType(Type type) {
        if (type == null) {
            return null;
        }
        if (type instanceof Class) {
            return (Class<?>)type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>)((ParameterizedType)type).getRawType();
        }
        throw new RuntimeException("Unexpected type: " + type);
    }

    public static Getter getGetter(Class<?> cls, String propertyName) {
        String capitalizedPropertyName = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);

        Method method = getAccessor(cls, capitalizedPropertyName);
        if (method == null) {
            method = getBooleanAccessor(cls, capitalizedPropertyName);
        }
        if (method == null) {
            method = getMethod(cls, propertyName);
        }
        if (method == null) {
            Field field;
            try {
                field = cls.getDeclaredField(propertyName);
            } catch (NoSuchFieldException e) {
                return null;
            }

            return new FieldGetter(field);
        }

        return new MethodGetter(method);
    }

    public static Setter getSetter(Class<?> cls, String propertyName) {
        String capitalizedPropertyName = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        try {
            String methodName = "set" + capitalizedPropertyName;
            Optional<Method> first = Arrays.stream(cls.getMethods())
                .filter(m -> m.getName().equals(methodName) && m.getReturnType().equals(void.class) && m.getParameters().length == 1)
                .findFirst();
            if (first.isPresent()) {
                return new MethodSetter(first.get());
            }
        } catch (Exception ignored) {
        }

        try {
            Field field = cls.getDeclaredField(propertyName);
            return new FieldSetter(field);
        } catch (NoSuchFieldException ignored) {
        }

        return null;
    }

    public static Optional<PropertyResolver> getPropertyResolver(Type type, String... propertyNames) {
        return PropertyResolver.from(type, propertyNames);
    }

    public static Method getSetterFromGetter(Method getter) {
        String name = getter.getName();
        String setterName;
        if (name.startsWith("is")) {
            setterName = "set" + name.substring(2);
        } else {
            setterName = "set" + name.substring(3);
        }
        try {
            return getter.getDeclaringClass().getMethod(setterName, getter.getReturnType());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static <T> T newInstance(Class<T> cls) {
        try {
            Constructor<?> constructor = Stream.of(cls.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 0)
                .findFirst()
                .orElseThrow(NoSuchMethodException::new);
            constructor.setAccessible(true);
            return cls.cast(constructor.newInstance());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor with zero parameters found on " + cls.getSimpleName(), e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * If the method given is part of a proxy (e.g. for validating purposes), and that proxy implements Supplier in
     * order to expose it's underlying implementation, then this will return the underlying Method definition, so that
     * param factories can search the implementing class for annotations.
     */
    public static Method getInstanceMethod(Method method, Object resourceInstance) {
        if (!Proxy.isProxyClass(method.getDeclaringClass())) {
            return method;
        }

        InvocationHandler invocationHandler = Proxy.getInvocationHandler(resourceInstance);
        if (!(invocationHandler instanceof Provider)) {
            return method;
        }

        resourceInstance = ((Provider)invocationHandler).get();
        Optional<Method> methodInClass = ReflectionUtil.findMethodInClass(method, resourceInstance.getClass());
        return methodInClass.orElse(method);
    }

    private static Method getAccessor(Class<?> cls, String propertyName) {
        return getMethod(cls, "get" + propertyName);
    }

    private static Method getBooleanAccessor(Class<?> cls, String propertyName) {
        Method method = getMethod(cls, "is" + propertyName);
        if (method == null) {
            return getMethod(cls, "has" + propertyName);
        }
        return method;
    }

    private static Method getMethod(Class<?> cls, String propertyName) {
        try {
            return cls.getMethod(propertyName, new Class[0]);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
