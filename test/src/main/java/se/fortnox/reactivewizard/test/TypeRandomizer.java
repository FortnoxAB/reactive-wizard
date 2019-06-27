package se.fortnox.reactivewizard.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

    public static Object getRandomizedType(Class<?> type) throws Exception {

        if (type.isEnum()) {
            Object[] enumValues = type.getEnumConstants();
            return enumValues[random.nextInt(enumValues.length)];
        }

        if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
            return random.nextInt(10);
        }

        if (type.equals(Long.TYPE) || type.equals(Long.class)) {
            return (long)random.nextInt(10);
        }

        if (type.equals(Double.TYPE) || type.equals(Double.class)) {
            return random.nextDouble();
        }

        if (type.equals(Float.TYPE) || type.equals(Float.class)) {
            return random.nextFloat();
        }

        if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
            return random.nextBoolean();
        }

        if (type.equals(String.class)) {
            return UUID.randomUUID().toString().substring(0,5);
        }

        if (type.equals(UUID.class)) {
            return UUID.randomUUID();
        }

        if (type.equals(BigInteger.class)) {
            return BigInteger.valueOf(random.nextInt(10));
        }

        if (type.equals(LocalDateTime.class)) {
            return LocalDateTime.now();
        }

        if (type.equals(LocalDate.class)) {
            return LocalDate.now();
        }
        if (type.equals(OffsetDateTime.class)) {
            return OffsetDateTime.now();
        }

        if (type.equals(List.class)) {
            return new ArrayList<>();
        }

        if (type.equals(Set.class)) {
            return new HashSet<>();
        }

        if (type.equals(Map.class)) {
            return new HashMap<>();
        }

        return createAndFill(type);
    }

    private static <T> T createAndFill(Class<T> clazz) throws Exception {
        Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
        declaredConstructor.setAccessible(true);
        T instance = declaredConstructor.newInstance();

        for (Field field: clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = getRandomizedType(field.getType());
            field.set(instance, value);
        }

        //Superclass with values? Yes, could do a recursive function to support extends of extends but, nah
        if (clazz.getSuperclass() != Object.class) {
            for (Field field : clazz.getSuperclass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = getRandomizedType(field.getType());
                field.set(instance, value);
            }
        }

        return instance;
    }
}
