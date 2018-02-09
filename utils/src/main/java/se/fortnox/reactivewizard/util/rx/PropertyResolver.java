package se.fortnox.reactivewizard.util.rx;

import se.fortnox.reactivewizard.util.Getter;
import se.fortnox.reactivewizard.util.ReflectionUtil;
import se.fortnox.reactivewizard.util.Setter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;

public class PropertyResolver {
    private final Type       genericType;
    private final Property[] properties;
    private final Class<?>   type;

    private PropertyResolver(Class<?> type, Type genericType, Property[] properties) {
        this.type = type;
        this.genericType = genericType;
        this.properties = properties;
    }

    public static Optional<PropertyResolver> from(Type type, String[] propertyNames) {
        Property[] property = new Property[propertyNames.length];
        Class<?>   cls      = ReflectionUtil.getRawType(type);
        for (int i = 0; i < propertyNames.length; i++) {
            property[i] = Property.from(cls, propertyNames[i]);
            if (property[i] == null) {
                // prop not found
                return Optional.empty();
            }
            cls = property[i].getType();
        }
        Type genericType = propertyNames.length == 0 ? type : property[propertyNames.length - 1].getGenericType();
        return Optional.of(new PropertyResolver(cls, genericType, property));
    }

    public Class<?> getPropertyType() {
        return type;
    }

    public Type getPropertyGenericType() {
        return genericType;
    }

    public Object getValue(Object value) {
        try {
            for (Property prop : properties) {
                value = prop.getValue(value);
            }
            return value;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<PropertyResolver> subPath(String[] subPath) {
        Optional<PropertyResolver> propsToAppend = from(getPropertyType(), subPath);
        if (propsToAppend.isPresent()) {
            PropertyResolver otherPropertyResolver = propsToAppend.get();
            Property[]       propertiesToAppend    = otherPropertyResolver.properties;
            Property[]       newProperties         = new Property[this.properties.length + propertiesToAppend.length];
            System.arraycopy(properties, 0, newProperties, 0, properties.length);
            System.arraycopy(propertiesToAppend, 0, newProperties, properties.length, newProperties.length);

            return Optional.of(new PropertyResolver(otherPropertyResolver.getPropertyType(), otherPropertyResolver.getPropertyGenericType(), newProperties));
        }
        return Optional.empty();
    }

    public void setValue(Object object, Object value) {
        try {
            for (int i = 0; i < properties.length - 1; i++) {
                Property property = properties[i];
                Object   next     = property.getValue(object);
                if (next == null) {
                    verifySetter(property);
                    next = ReflectionUtil.newInstance(property.getType());
                    property.setValue(object, next);
                }
                object = next;
            }
            Property property = properties[properties.length - 1];
            verifySetter(property);
            property.setValue(object, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void verifySetter(Property property) {
        if (!property.hasSetter()) {
            throw new RuntimeException("Value from " + property.getName() + " was null and there is no setter");
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(properties);
    }

    private static class Property {
        private final String   name;
        private final Class<?> type;
        private final Type     genericType;
        private final Getter   getter;
        private final Setter   setter;

        Property(String name, Class<?> type, Type genericType, Getter getter, Setter setter) {
            this.name = name;
            this.type = type;
            this.genericType = genericType;
            this.getter = getter;
            this.setter = setter;
        }

        private static Property from(Class<?> cls, String prop) {
            final Getter getter = ReflectionUtil.getGetter(cls, prop);
            final Setter setter = ReflectionUtil.getSetter(cls, prop);

            if (getter != null) {
                return new Property(prop, getter.getReturnType(), getter.getGenericReturnType(), getter, setter);
            } else if (setter != null) {
                return new Property(prop, setter.getParameterType(), setter.getGenericParameterType(), null, setter);
            }

            return null;
        }

        public Class<?> getType() {
            return type;
        }

        Type getGenericType() {
            return genericType;
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
