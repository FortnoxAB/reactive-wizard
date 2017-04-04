package se.fortnox.reactivewizard.util.rx;

import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Created by jonashall on 2015-12-08.
 */
public class PropertyResolver {
	private final Type genericType;
	private final Property[] properties;
	private final Class<?> type;

	private PropertyResolver(Class<?> type, Type genericType, Property[] properties) {
		this.type = type;
		this.genericType = genericType;
		this.properties = properties;
	}

	public static Optional<PropertyResolver> from(Type type, String[] propertyNames) {
		Property[] props = new Property[propertyNames.length];
		Class<?> cls = ReflectionUtil.getRawType(type);
		for (int i = 0; i < propertyNames.length; i++) {
			props[i] = Property.from(cls, propertyNames[i]);
			if (props[i] == null) {
				// prop not found
				return Optional.empty();
			}
			cls = props[i].getType();
		}
		Type genericType = propertyNames.length == 0 ? type : props[propertyNames.length-1].getGenericType();
		return Optional.of(new PropertyResolver(cls, genericType, props));
	}

	public Class<?> getPropertyType() {
		return type;
	}

	public Type getPropertyGenericType() {
		return genericType;
	}

	public Object getValue(Object val){
		try {
			for (Property prop : properties) {
				val = prop.getValue(val);
			}
			return val;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Optional<PropertyResolver> subPath(String[] subPath) {
		Optional<PropertyResolver> propsToAppend = from(getPropertyType(), subPath);
		if (propsToAppend.isPresent()) {
			PropertyResolver otherPropResolver = propsToAppend.get();
			Property[] appendProps = otherPropResolver.properties;
			Property[] newProps = new Property[this.properties.length+appendProps.length];
			System.arraycopy(properties, 0, newProps, 0, properties.length);
			System.arraycopy(appendProps, 0, newProps, properties.length, newProps.length);

			return Optional.of(new PropertyResolver(otherPropResolver.getPropertyType(), otherPropResolver.getPropertyGenericType(), newProps));
		}
		return Optional.empty();
	}

	public void setValue(Object obj, Object val) {
		try {
			for (int i = 0; i < properties.length-1; i++) {
				Property prop = properties[i];
				Object next = prop.getValue(obj);
				if (next == null) {
					verifySetter(prop);
					next = prop.getType().newInstance();
					prop.setValue(obj, next);
				}
				obj = next;
			}
			Property prop = properties[properties.length - 1];
			verifySetter(prop);
			prop.setValue(obj, val);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void verifySetter(Property prop) {
		if (!prop.hasSetter()) {
			throw new RuntimeException("Value from " + prop.getName() + " was null and there is no setter");
		}
	}

	@Override
	public String toString() {
		return properties.toString();
	}

	private static class Property {

		private final String name;
		private Class<?> type;
		private Type genericType;
		private final Method getter;
		private final Method setter;

		Property(String name, Class<?> type, Type genericType, Method getter, Method setter) {
			this.name = name;
			this.type = type;
			this.genericType = genericType;
			this.getter = getter;
			this.setter = setter;
		}

		public Class<?> getType() {
			return type;
		}

		Type getGenericType() {
			return genericType;
		}

		private static Property from(Class<?> cls, String prop) {
			final Method getter = ReflectionUtil.getGetter(cls, prop);
			final Method setter = ReflectionUtil.getSetter(cls, prop);

			if (getter != null) {
				return new Property(prop, getter.getReturnType(), getter.getGenericReturnType(), getter, setter);
			} else if (setter != null) {
				return new Property(prop, setter.getParameterTypes()[0], setter.getGenericParameterTypes()[0], null, setter);
			}

			return null;
		}

		Object getValue(Object instance) throws InvocationTargetException, IllegalAccessException {
			return getter.invoke(instance);
		}

		boolean hasSetter() {
			return setter != null;
		}

		String getName() {
			return name;
		}

		void setValue(Object instance, Object value) throws InvocationTargetException, IllegalAccessException {
			setter.invoke(instance, value);
		}

		@Override
		public String toString() {
			return name;
		}
	}
}