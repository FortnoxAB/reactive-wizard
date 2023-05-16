package se.fortnox.reactivewizard.util;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class ReflectionUtil {
    private static final String CGLIB_CLASS_SEPARATOR = "$$";

    /**
     * Get the type of Flux/Mono from method.
     * @param method the method
     * @return the type of Flux/Mono
     */
    public static Type getTypeOfFluxOrMono(Method method) {
        Type type = method.getGenericReturnType();
        if (!(type instanceof ParameterizedType)) {
            method = getInterfaceMethod(method);
            if (method == null) {
                throw new RuntimeException("method does not have a generic return type");
            }
            type = method.getGenericReturnType();
        }
        ParameterizedType parameterizedType = (ParameterizedType)type;
        Class<?>          rawClass          = (Class<?>)parameterizedType.getRawType();
        if (!FluxRxConverter.isReactiveType(rawClass)) {
            throw new RuntimeException(type + " is not a reactive type");
        }
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        return actualTypeArguments[0];
    }

    /**
     * Get class of generic parameter.
     * @param type the type
     * @return the class
     */
    public static Class<?> getGenericParameter(Type type) {
        if (!(type instanceof ParameterizedType)) {
            throw new RuntimeException("The sent in type " + type + " is not a ParameterizedType");
        }
        ParameterizedType pt                  = (ParameterizedType)type;
        Type[]            actualTypeArguments = pt.getActualTypeArguments();
        if (actualTypeArguments.length != 1) {
            throw new RuntimeException("The sent in type " + type + " should have exactly one type argument, but had " + actualTypeArguments.length);
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
     * Find overridden method.
     * @param method the method that overrides
     * @return the overridden method or null if none found
     */
    public static Method getOverriddenMethod(Method method) {
        Class<?> declaringClass = getUserDefinedClass(method.getDeclaringClass());
        Optional<Method> found;
        for (Class<?> iface : declaringClass.getInterfaces()) {
            found = findMethodInClass(method, iface);
            if (found.isPresent()) {
                return found.get();
            }
        }
        found = findMethodInClass(method, declaringClass);
        found = found.filter(m -> !m.equals(method));
        return found.orElse(null);
    }

    /**
     * Fetches the (possibly) redefined version of a method for a class that has been hot-reloaded.
     *
     * Intended to be used while debugging stuff and using the hot reload feature in the IDE.
     * @param method the method that may have been redefined
     * @return a redefined version of that method, or the passed in method if no new version is found
     */
    public static Method getRedefinedMethod(Method method) {
        Class<?> declaringClass = getUserDefinedClass(method.getDeclaringClass());
        return findMethodInClass(method, declaringClass).orElse(method);
    }

    private static Class<?> getUserDefinedClass(Class<?> clazz) {
        if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }
        return clazz;
    }

    /**
     * Find method in class matching name and parameter types.
     * @param method the method to match
     * @param cls the class to search
     * @return an Optional of method
     */
    public static Optional<Method> findMethodInClass(Method method, Class<?> cls) {
        for (Method candidate : cls.getDeclaredMethods()) {
            if (methodsEquals(method, candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * Find annotation on method or overridden methods thereof.
     * @param method the method
     * @param annotationClass the annotation class to look for
     * @param <T> the type of annotation
     * @return the annotation or null if not found
     */
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

    /**
     * Get all annotations for method or overridden methods thereof.
     * @param method the method
     * @return the annotations
     */
    public static List<Annotation> getAnnotations(Method method) {
        List<Annotation> result     = new LinkedList<>(asList(method.getAnnotations()));
        Method           overridden = getOverriddenMethod(method);
        if (overridden != null) {
            result.addAll(asList(overridden.getAnnotations()));
        }
        return result;
    }

    /**
     * Get annotations of the parameters of a method or overridden methods thereof.
     * @param method the method
     * @return the annotations
     */
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

    /**
     * Get annotations for parameter.
     * @param parameter the parameter
     * @return the annotations
     */
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

    /**
     * Get the type object representing the class or interface that declared a type.
     * @param type the type
     * @return the raw type
     */
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

    /**
     * Locate a getter (method or field) for a property.
     * <p>
     * The class hierarchy will be traversed to find any inherited getter for the given property.
     *
     * @param cls          The class inspected.
     * @param propertyName The property to locate a getter for.
     * @return a Getter instance for either a method or a field
     */
    static Getter getGetter(Class<?> cls, String propertyName) {
        return getGetter(cls, cls, propertyName);
    }

    /**
     * Recurse through the class hierarchy to find a getter for a property.
     *
     * @param original       The original class inspected for a getter.
     * @param declaringClass The current class in the hierarchy being inspected.
     * @param propertyName   The property to locate a getter for.
     * @return a Getter instance for either a method or a field
     */
    private static Getter getGetter(Class<?> original, Class<?> declaringClass, String propertyName) {
        String capitalizedPropertyName = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);

        Method method = getAccessor(declaringClass, capitalizedPropertyName);
        if (method == null) {
            method = getBooleanAccessor(declaringClass, capitalizedPropertyName);
        }
        if (method == null) {
            method = getMethod(declaringClass, propertyName);
        }
        if (method == null) {
            Field field;
            try {
                field = declaringClass.getDeclaredField(propertyName);
            } catch (NoSuchFieldException e) {
                if (declaringClass.getSuperclass() != null) {
                    return getGetter(original, declaringClass.getSuperclass(), propertyName);
                }
                return null;
            }

            return FieldGetter.create(original, field);
        }

        return MethodGetter.create(original, method);
    }

    /**
     * Locate a setter (method or field) for a property.
     * <p>
     * The class hierarchy will be traversed to find any inherited setter for the given property.
     *
     * @param cls          The class inspected.
     * @param propertyName The property to locate a setter for.
     * @return a Setter instance for either a method or a field
     */
    static Setter getSetter(Class<?> cls, String propertyName) {
        return getSetter(cls, cls, propertyName);
    }

    /**
     * Recurse through the class hierarchy to find a setter for a property.
     *
     * @param original       The original class inspected for a setter.
     * @param declaringClass The current class in the hierarchy being inspected.
     * @param propertyName   The property to locate a setter for.
     * @return a Setter instance for either a method or a field
     */
    private static Setter getSetter(Class<?> original, Class<?> declaringClass, String propertyName) {
        String capitalizedPropertyName = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        try {
            String methodName = "set" + capitalizedPropertyName;
            Optional<Method> first = Arrays.stream(declaringClass.getMethods())
                .filter(m -> !m.isSynthetic() && m.getName().equals(methodName) && m.getReturnType().equals(void.class) && m.getParameterCount() == 1)
                .findFirst();
            if (first.isPresent()) {
                return MethodSetter.create(original, first.get());
            }
        } catch (Exception ignored) {
        }

        try {
            Field field = declaringClass.getDeclaredField(propertyName);
            return FieldSetter.create(original, field);
        } catch (NoSuchFieldException ignored) {
            if (declaringClass.getSuperclass() != null) {
                return getSetter(original, declaringClass.getSuperclass(), propertyName);
            }
        }

        return null;
    }

    public static Optional<PropertyResolver> getPropertyResolver(Type type, String... propertyNames) {
        return PropertyResolver.from(type, propertyNames);
    }

    /**
     * If the method given is part of a proxy (e.g. for validating purposes), and that proxy implements Supplier in
     * order to expose its underlying implementation, then this will return the underlying Method definition, so that
     * param factories can search the implementing class for annotations.
     * @param method the method
     * @param resourceInstance the resource instance
     * @return the method
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
            return cls.getDeclaredMethod(propertyName);
        } catch (NoSuchMethodException e) {
            if (cls.getSuperclass() != null) {
                return getMethod(cls.getSuperclass(), propertyName);
            }
            return null;
        }
    }

    /**
     * Get instantiator for class.
     * @param cls the class
     * @param <T> the type of class
     * @return the instantiator in the form of a Supplier
     */
    public static <T> Supplier<T> instantiator(Class<T> cls) {
        try {
            Constructor<?> constructor = Stream.of(cls.getDeclaredConstructors())
                    .filter(c -> c.getParameterCount() == 0)
                    .findFirst()
                    .orElseThrow(NoSuchMethodException::new);
            MethodHandles.Lookup lookup = lookupFor(cls, constructor);
            constructor.setAccessible(true);
            MethodHandle methodHandle = lookup.unreflectConstructor(constructor);
            return LambdaCompiler.compileLambdaSupplier(lookup, methodHandle);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor with zero parameters found on " + cls.getSimpleName(), e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static <T> MethodHandles.Lookup lookupFor(Class<T> cls, AccessibleObject accessibleObject) {
        try {
            final MethodHandles.Lookup original = MethodHandles.lookup();
            if (accessibleObject.isAccessible()) {
                return original;
            }
            // Change the lookup to allow private access
            final Field internal = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            internal.setAccessible(true);
            final MethodHandles.Lookup trusted = (MethodHandles.Lookup) internal.get(original);

            return trusted.in(cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <I,T> Optional<Function<I,T>> getter(Class<I> instanceCls, String propertyPath) {
        Optional<PropertyResolver> propertyResolver = ReflectionUtil.getPropertyResolver(instanceCls, propertyPath.split("\\."));
        return propertyResolver.map(PropertyResolver::getter);
    }

    public static <I,T> Optional<BiConsumer<I,T>> setter(Class<I> instanceCls, String propertyPath) {
        Optional<PropertyResolver> propertyResolver = ReflectionUtil.getPropertyResolver(instanceCls, propertyPath.split("\\."));
        return propertyResolver.map(PropertyResolver::setter);

    }
}
