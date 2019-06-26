package se.fortnox.reactivewizard.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TypeRandomizer {
    private static final Random random = new Random();

    public static Object getRandomizedType(Class<?> type) throws Exception {

        if (type.isEnum()) {
            Object[] enumValues = type.getEnumConstants();
            return enumValues[random.nextInt(enumValues.length)];
        } else if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
            return random.nextInt(10);
        } else if (type.equals(Long.TYPE) || type.equals(Long.class)) {
            return (long)random.nextInt(10);
        } else if (type.equals(Double.TYPE) || type.equals(Double.class)) {
            return random.nextDouble();
        } else if (type.equals(Float.TYPE) || type.equals(Float.class)) {
            return random.nextFloat();
        } else if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
            return random.nextBoolean();
        } else if (type.equals(String.class)) {
            return UUID.randomUUID().toString().substring(0,5);
        } else if (type.equals(UUID.class)) {
            return UUID.randomUUID();
        } else if (type.equals(BigInteger.class)) {
            return BigInteger.valueOf(random.nextInt(10));
        } else if (type.equals(LocalDateTime.class)) {
            return LocalDateTime.now();
        } else if (type.equals(LocalDate.class)) {
            return LocalDate.now();
        } else if (type.equals(OffsetDateTime.class)) {
            return OffsetDateTime.now();
        } else if (type.equals(List.class)) {
            return new ArrayList<>();
        } else if (type.equals(Map.class)) {
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
