package se.fortnox.reactivewizard.test;

import com.google.common.collect.ImmutableMap;

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
import java.util.function.Supplier;

public class TypeRandomizer {
    private TypeRandomizer() {}

    private static final Random random = new Random();
    private static final Map<Class<?>, Supplier<?>>  suppliers = ImmutableMap.<Class<?>, Supplier<?>>builder()
        .put(Integer.TYPE, () -> random.nextInt(10))
        .put(Integer.class, () -> random.nextInt(10))
        .put(Float.TYPE, random::nextFloat)
        .put(Float.class, random::nextFloat)
        .put(Long.TYPE, () -> (long)random.nextInt(10))
        .put(Long.class, () -> (long)random.nextInt(10))
        .put(Double.TYPE, random::nextDouble)
        .put(Double.class, random::nextDouble)
        .put(Boolean.TYPE, random::nextBoolean)
        .put(Boolean.class, random::nextBoolean)
        .put(String.class, () -> UUID.randomUUID().toString().substring(0,5))
        .put(UUID.class, UUID::randomUUID)
        .put(BigInteger.class, () -> BigInteger.valueOf(random.nextInt(10)))
        .put(LocalDateTime.class, LocalDateTime::now)
        .put(LocalDate.class, LocalDate::now)
        .put(OffsetDateTime.class, OffsetDateTime::now)
        .put(List.class, ArrayList::new)
        .put(Map.class, HashMap::new)
        .put(Set.class, HashSet::new)
        .build();



    @SuppressWarnings("unchecked")
    public static <T> T getType(Class<T> type) {
        if (type.isEnum()) {
            T[] enumValues = type.getEnumConstants();
            return enumValues[random.nextInt(enumValues.length)];
        }

        if (suppliers.containsKey(type)) {
            return (T)suppliers.get(type).get();
        }

        return createAndPopulateObjectWithValues(type);
    }

    private static <T> T createAndPopulateObjectWithValues(Class<T> clazz) {
        try {
            Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            T instance = declaredConstructor.newInstance();

            for (Field field: clazz.getDeclaredFields()) {
                setRandomValueOnField(instance, field);
            }

            //Superclass with values? Yes, could do a recursive function to support extends of extends but, nah
            if (clazz.getSuperclass() != Object.class) {
                for (Field field : clazz.getSuperclass().getDeclaredFields()) {
                    setRandomValueOnField(instance, field);
                }
            }

            return instance;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Could not instantiate class: " + clazz + " " + e.getMessage());
        }
    }

    private static <T> void setRandomValueOnField(T instance, Field field) throws IllegalAccessException {
        if (field.getName().startsWith("$")) {
            return;
        }

        field.setAccessible(true);
        Object value = getType(field.getType());
        field.set(instance, value);
    }
}
