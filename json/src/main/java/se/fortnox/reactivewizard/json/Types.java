package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Type;

/**
 * Helper for working with reflection.
 */
public class Types {
    /**
     * Turns an existing type into a jackson TypeReference.
     * @param type is the type from reflection
     * @param <T> is the generic type
     * @return a type reference of the given Type
     */
    public static <T> TypeReference<T> toReference(Type type) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return type;
            }
        };
    }
}
