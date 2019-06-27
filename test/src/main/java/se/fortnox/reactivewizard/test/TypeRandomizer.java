package se.fortnox.reactivewizard.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public abstract class TypeRandomizer {
    private TypeRandomizer() {}

    private static final Random random = new Random();

    @SuppressWarnings("unchecked")
    public static <T> T getType(Class<T> type) {

        if (type.isEnum()) {
            T[] enumValues = type.getEnumConstants();
            return enumValues[random.nextInt(enumValues.length)];
        }

        if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
            return (T)Integer.valueOf(random.nextInt(10));
        }

        if (type.equals(Long.TYPE) || type.equals(Long.class)) {
            return (T)Long.valueOf((long)random.nextInt(10));
        }

        if (type.equals(Double.TYPE) || type.equals(Double.class)) {
            return (T)Double.valueOf(random.nextDouble());
        }

        if (type.equals(Float.TYPE) || type.equals(Float.class)) {
            return (T)Float.valueOf(random.nextFloat());
        }

        if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
            return (T)Boolean.valueOf(random.nextBoolean());
        }

        if (type.equals(String.class)) {
            return (T)UUID.randomUUID().toString().substring(0,5);
        }

        if (type.equals(UUID.class)) {
            return (T)UUID.randomUUID();
        }

        if (type.equals(BigInteger.class)) {
            return (T)BigInteger.valueOf(random.nextInt(10));
        }

        if (type.equals(LocalDateTime.class)) {
            return (T)LocalDateTime.now();
        }

        if (type.equals(LocalDate.class)) {
            return (T)LocalDate.now();
        }
        if (type.equals(OffsetDateTime.class)) {
            return (T)OffsetDateTime.now();
        }

        if (type.equals(List.class)) {
            return (T)new ArrayList();
        }

        if (type.equals(Set.class)) {
            return (T)new HashSet();
        }

        if (type.equals(Map.class)) {
            return (T) new HashMap();
        }

        return createAndFill(type);
    }

    private static <T> T createAndFill(Class<T> clazz) {
        try {
            Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            T instance = declaredConstructor.newInstance();

            for (Field field: clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = getType(field.getType());
                field.set(instance, value);
            }

            //Superclass with values? Yes, could do a recursive function to support extends of extends but, nah
            if (clazz.getSuperclass() != Object.class) {
                for (Field field : clazz.getSuperclass().getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = getType(field.getType());
                    field.set(instance, value);
                }
            }

            return instance;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Could not instantiate class: " + clazz + " " + e.getMessage());
        }
    }
}
