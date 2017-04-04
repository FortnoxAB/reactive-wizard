package se.fortnox.reactivewizard.util;

import se.fortnox.reactivewizard.util.rx.PropertyResolver;
import rx.Observable;
import rx.Single;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.Arrays.asList;

public class ReflectionUtil {

	public static Type getTypeOfObservable(Method m) {
		Type t = m.getGenericReturnType();
		if (!(t instanceof ParameterizedType)) {
			m = getInterfaceMethod(m);
			t = m.getGenericReturnType();
		}
		ParameterizedType pt = (ParameterizedType) t;
		Class<?> rawCls = (Class<?>) pt.getRawType();
		if (!(rawCls.equals(Observable.class)) && !(rawCls.equals(Single.class))) {
			throw new RuntimeException(t + " is not an Observable or Single");
		}
		Type[] actualTypeArguments = pt.getActualTypeArguments();
		return actualTypeArguments[0];
	}

	public static Class<?> getGenericParameter(Type type) {
		if (!(type instanceof ParameterizedType)) {
			throw new RuntimeException("The sent in type "+type+" is not a ParameterizedType");
		}
		ParameterizedType pt = (ParameterizedType) type;
		Type[] actualTypeArguments = pt.getActualTypeArguments();
		if (actualTypeArguments.length != 1) {
			throw new RuntimeException("The sent in type "+type+" should have exactly one type argument, but had "+actualTypeArguments);
		}
		return (Class<?>)actualTypeArguments[0];
	}

	private static Method getInterfaceMethod(Method m) {
		for (Class iface : m.getDeclaringClass().getInterfaces()) {
			for (Method candidate : iface.getDeclaredMethods()) {
				if (methodsEquals(m, candidate)) {
					return candidate;
				}
			}
		}
		return null;
	}

	private static boolean methodsEquals(Method m, Method candidate) {
		return candidate.getName().equals(m.getName()) && Arrays.equals(candidate.getParameterTypes(), m.getParameterTypes());
	}

	/**
	 * Extracts a named property from an object using reflection
	 *
	 * @param obj
	 * @param prop
	 * @return
	 */
	public static Object getValue(Object obj, String prop) {
		Class<? extends Object> cls = obj.getClass();
		prop = prop.substring(0, 1).toUpperCase() + prop.substring(1);
		try {
			try {
				return cls.getMethod("get" + prop, new Class[0]).invoke(obj);
			} catch (NoSuchMethodException e) {
				return cls.getMethod("is" + prop, new Class[0]).invoke(obj);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Method getOverriddenMethod(Method m) {
		Optional<Method> found;
		for (Class<?> iface : m.getDeclaringClass().getInterfaces()) {
			found = findMethodInClass(m, iface);
			if (found.isPresent()) return found.get();
		}
		found = findMethodInClass(m, m.getDeclaringClass());
		if (!found.isPresent()) {
			return null;
		}
		return found.get();
	}

	public static Optional<Method> findMethodInClass(Method m, Class<?> cls) {
		for (Method candidate : cls.getDeclaredMethods()) {
            if (methodsEquals(m, candidate)) {
                return Optional.of(candidate);
            }
        }
		return Optional.empty();
	}

	public static <T extends Annotation> T getAnnotation(Method method,
			Class<T> annotationClass) {
		T annotation = method.getAnnotation(annotationClass);

		if (annotation == null) {
			Method overriddenMethod = getOverriddenMethod(method);
			if (overriddenMethod != null) {
				annotation = overriddenMethod.getAnnotation(annotationClass);
			}
		}

		return annotation;
	}

	public static List<Annotation> getAnnotations(Method m) {
		List<Annotation> result = new LinkedList<>(asList(m.getAnnotations()));
		Method overridden = getOverriddenMethod(m);
		if (overridden != null) {
			result.addAll(asList(overridden.getAnnotations()));
		}
		return result;
	}

	public static List<List<Annotation>> getParameterAnnotations(Method m) {
		List<List<Annotation>> result = new LinkedList<List<Annotation>>();
		Annotation[][] annot = m.getParameterAnnotations();
		Method overridden = getOverriddenMethod(m);
		Annotation[][] inheritedAnnot = null;
		if (overridden != null) {
			inheritedAnnot = overridden.getParameterAnnotations();
		}
		for (int i = 0; i < annot.length; i++) {
			List<Annotation> merged = new LinkedList<Annotation>();
			merged.addAll(asList(annot[i]));
			if (inheritedAnnot != null) {
				merged.addAll(asList(inheritedAnnot[i]));
			}
			result.add(merged);
		}

		return result;
	}

	public static Class<?> getRawType(Type t) {
		if (t == null) {
			return null;
		}
		if (t instanceof Class) {
			return (Class<?>) t;
		}
		if (t instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) t).getRawType();
		}
		throw new RuntimeException("Unexpected type: " + t);
	}


	public static List<Annotation> getParameterAnnotations(Parameter parameter) {
		List<Annotation> annotations = new ArrayList<>(asList(parameter.getAnnotations()));
		Method method = (Method) parameter.getDeclaringExecutable();
		Method overriddenMethod = getOverriddenMethod(method);
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

	public static Method getGetter(Class<?> cls, String propertyName) {
		String capitalizedPropertyName = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);

		Method method = getAccessor(cls, capitalizedPropertyName);
		if (method == null) {
			method = getBooleanAccessor(cls, capitalizedPropertyName);
		}
		if (method == null) {
			method = getMethod(cls, propertyName);
		}
		return method;
	}

	public static Method getSetter(Class<?> cls, String propertyName) {
		propertyName = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
		try {
			String methodName = "set" + propertyName;
			Optional<Method> first = Arrays.stream(cls.getMethods())
					.filter(m -> m.getName().equals(methodName) && m.getReturnType().equals(void.class) && m.getParameters().length == 1)
					.findFirst();
			if (first.isPresent()) {
				return first.get();
			}
		} catch (Exception e) {
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
			setterName = "set"+name.substring(2);
		} else {
			setterName = "set"+name.substring(3);
		}
		try {
			return getter.getDeclaringClass().getMethod(setterName, getter.getReturnType());
		} catch (NoSuchMethodException e) {
			return null;
		}
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
