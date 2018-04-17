package se.fortnox.reactivewizard.db.deserializing;

import java.lang.reflect.Modifier;
import java.util.stream.Stream;

/**
 * A simple way to define immutability (that suffices for our purposes) is that the class should not have setters, a
 * no-arg constructor or non-final fields.
 */
class MutabilityDetector {
    static boolean isImmutable(Class<?> cls) {
        return !hasSetterMethod(cls) && !hasNoArgConstructor(cls) && !hasNonFinalField(cls);
    }

    private static boolean hasNoArgConstructor(Class<?> cls) {
        return Stream.of(cls.getConstructors())
            .anyMatch(constructor -> constructor.getParameterCount() == 0 && !constructor.isSynthetic());
    }

    private static boolean hasSetterMethod(Class<?> cls) {
        return Stream.of(cls.getMethods())
            .anyMatch(method -> method.getName().startsWith("set") && method.getParameterCount() == 1
                && !method.isSynthetic());
    }

    private static boolean hasNonFinalField(Class<?> cls) {
        return Stream.of(cls.getDeclaredFields())
            .anyMatch(field -> !Modifier.isFinal(field.getModifiers()) && !field.isSynthetic());
    }
}
