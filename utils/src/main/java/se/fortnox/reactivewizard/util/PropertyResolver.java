package se.fortnox.reactivewizard.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PropertyResolver<I,T> {
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

    public Optional<PropertyResolver> subPath(String[] subPath) {
        Optional<PropertyResolver> propsToAppend = from(getPropertyType(), subPath);
        if (propsToAppend.isPresent()) {
            PropertyResolver otherPropertyResolver = propsToAppend.get();
            Property[]       propertiesToAppend    = otherPropertyResolver.properties;
            Property[]       newProperties         = new Property[this.properties.length + propertiesToAppend.length];
            System.arraycopy(properties, 0, newProperties, 0, properties.length);
            System.arraycopy(propertiesToAppend, 0, newProperties, properties.length, propertiesToAppend.length);

            return Optional.of(new PropertyResolver(otherPropertyResolver.getPropertyType(), otherPropertyResolver.getPropertyGenericType(), newProperties));
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return Arrays.toString(properties);
    }

    public Function<I,T> getter() {
        if (properties.length == 0) {
            return (Function<I, T>) Function.identity();
        }
        if (properties.length == 1) {
            return properties[0].getter();
        }

        Function[] functionChain = new Function[properties.length];
        for (int i = 0; i < properties.length; i++) {
            functionChain[i] = properties[i].getter();
        }
        return new FunctionChain<>(functionChain);
    }

    public BiConsumer<I,T> setter() {
        if (properties.length == 0) {
            throw new IllegalArgumentException("No properties found");
        }
        if (properties.length == 1) {
            return properties[0].setter();
        }

        Function[] getterChain = new Function[properties.length - 1];
        BiConsumer[] setterChain = new BiConsumer[properties.length - 1];
        Supplier[] instantiators = new Supplier[properties.length - 1];
        for (int i = 0; i < properties.length - 1; i++) {
            getterChain[i] = properties[i].getter();
            setterChain[i] = properties[i].setter();
            instantiators[i] = ReflectionUtil.instantiator(properties[i].getType());
        }
        return new SetterChain(getterChain, setterChain, instantiators, properties[properties.length - 1].setter());
    }

    private static class FunctionChain<I,T> implements Function<I,T> {

        private final Function[] functionChain;

        public FunctionChain(Function[] functionChain) {
            this.functionChain = functionChain;
        }

        @Override
        public T apply(I instance) {
            Object currentInstance = instance;
            for (int i = 0; i < functionChain.length && currentInstance != null; i++) {
                currentInstance = functionChain[i].apply(currentInstance);
            }
            return (T)currentInstance;
        }
    }

    private static class SetterChain<I,T> implements BiConsumer<I,T> {

        private final Function[] getterChain;
        private final BiConsumer[] setterChain;
        private final Supplier[] instantiators;
        private final BiConsumer setter;

        public SetterChain(Function[] getterChain, BiConsumer[] setterChain, Supplier[] instantiators, BiConsumer setter) {
            this.getterChain = getterChain;
            this.setterChain = setterChain;
            this.instantiators = instantiators;
            this.setter = setter;
        }

        @Override
        public void accept(I instance, T value) {
            Object currentInstance = instance;
            for (int i = 0; i < getterChain.length && currentInstance != null; i++) {
                Object next = getterChain[i].apply(currentInstance);
                if (next == null) {
                    next = instantiators[i].get();
                    setterChain[i].accept(currentInstance, next);
                }
                currentInstance = next;
            }
            setter.accept(currentInstance, value);
        }
    }

    private static class Property<I,T> {
        private final String      name;
        private final Class<?>    type;
        private final Type        genericType;
        private final Getter<I,T> getter;
        private final Setter<I,T> setter;

        Property(String name, Class<?> type, Type genericType, Getter getter, Setter setter) {
            this.name = name;
            this.type = type;
            this.genericType = genericType;
            this.getter = getter;
            this.setter = setter;
        }

        private static <I,T> Property<I,T> from(Class<I> cls, String prop) {
            final Getter<I,T> getter = ReflectionUtil.getGetter(cls, prop);
            final Setter<I,T> setter = ReflectionUtil.getSetter(cls, prop);

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

        @Override
        public String toString() {
            return name;
        }

        public Function<I,T> getter() {
            return getter.getterFunction();
        }

        public BiConsumer<I, T> setter() {
            return setter.setterFunction();
        }
    }
}
