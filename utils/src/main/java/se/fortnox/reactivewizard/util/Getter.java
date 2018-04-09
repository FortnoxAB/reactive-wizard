package se.fortnox.reactivewizard.util;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for a getter.
 */
public interface Getter {
    /**
     * Create a map of all generic type parameters on a super class to the actual types in a subclass.
     *
     * @param cls    The subclass to find out type information for
     * @param member The member (field or method) to find out type information for
     * @return a mapping of the generic type names (ie. "T") to the actual types
     */
    static Map<String, Class<?>> typesByGenericName(Class<?> cls, Member member) {
        Map<String, Class<?>> typeByName = new HashMap<>();
        for (int i = 0; i < member.getDeclaringClass().getTypeParameters().length; i++) {
            String typeName = member.getDeclaringClass().getTypeParameters()[i].getTypeName();
            Type genericSuperclass = cls.getGenericSuperclass();
            if (genericSuperclass != null) {
                Class<?> type = (Class<?>) ((ParameterizedTypeImpl) genericSuperclass).getActualTypeArguments()[i];
                typeByName.put(typeName, type);
            }
        }
        return typeByName;
    }

    Object invoke(Object instance) throws InvocationTargetException, IllegalAccessException;

    Class<?> getReturnType();

    Type getGenericReturnType();
}

