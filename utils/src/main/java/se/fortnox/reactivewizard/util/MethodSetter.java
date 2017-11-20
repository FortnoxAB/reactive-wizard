package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class MethodSetter implements Setter {
	private final Method method;

	public MethodSetter(Method method) {
		this.method = method;
	}

	@Override
	public void invoke(Object instance, Object value) throws InvocationTargetException, IllegalAccessException {
		method.invoke(instance, value);
	}

	@Override
	public Class<?> getParameterType() {
		return method.getParameterTypes()[0];
	}

	@Override
	public Type getGenericParameterType() {
		return method.getGenericParameterTypes()[0];
	}
}
