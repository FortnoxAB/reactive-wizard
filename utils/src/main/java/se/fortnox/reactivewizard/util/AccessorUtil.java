package se.fortnox.reactivewizard.util;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

class AccessorUtil {
    /**
     * Create a map of all generic type parameters on a super class to the actual types in a subclass.
     *
     * @param subClass The subclass to find out type information for
     * @param member   The member (field or method) to find out type information for
     * @return a mapping of the generic type names (ie. "T") to the actual types
     */
    private static Map<String, Class<?>> typesByGenericName(Class<?> subClass, Member member) {
        Map<String, Class<?>> typeByName = new HashMap<>();
        for (int i = 0; i < member.getDeclaringClass().getTypeParameters().length; i++) {
            String typeName = member.getDeclaringClass().getTypeParameters()[i].getTypeName();
            Type genericSuperclass = subClass.getGenericSuperclass();
            if (genericSuperclass != null) {
                Class<?> type = (Class<?>) ((ParameterizedTypeImpl) genericSuperclass).getActualTypeArguments()[i];
                typeByName.put(typeName, type);
            }
        }
        return typeByName;
    }

    static MemberTypeInfo fieldTypeInfo(Class<?> cls, Field field) {
        Map<String, Class<?>> genericTypenameToType = AccessorUtil.typesByGenericName(cls, field);
        Class<?> genericType = genericTypenameToType.get(field.getGenericType().getTypeName());

        Class<?> returnType;
        Type genericReturnType;
        if (genericType == null) {
            returnType = field.getType();
            genericReturnType = field.getGenericType();
        } else {
            returnType = genericType;
            genericReturnType = genericType;
        }

        return new MemberTypeInfo(returnType, genericReturnType);
    }

    static MemberTypeInfo getterTypeInfo(Class<?> cls, Method method) {
        Map<String, Class<?>> genericTypenameToType = AccessorUtil.typesByGenericName(cls, method);
        Class<?> genericType = genericTypenameToType.get(method.getGenericReturnType().getTypeName());

        Class<?> returnType;
        Type genericReturnType;
        if (genericType == null) {
            returnType = method.getReturnType();
            genericReturnType = method.getGenericReturnType();
        } else {
            returnType = genericType;
            genericReturnType = genericType;
        }

        return new MemberTypeInfo(returnType, genericReturnType);
    }

    static MemberTypeInfo setterTypeInfo(Class<?> cls, Method method) {
        Map<String, Class<?>> genericTypenameToType = AccessorUtil.typesByGenericName(cls, method);
        Class<?> genericType = genericTypenameToType.get(method.getGenericParameterTypes()[0].getTypeName());

        Class<?> returnType;
        Type genericReturnType;
        if (genericType == null) {
            returnType = method.getParameterTypes()[0];
            genericReturnType = method.getGenericParameterTypes()[0];
        } else {
            returnType = genericType;
            genericReturnType = genericType;
        }

        return new MemberTypeInfo(returnType, genericReturnType);
    }

    static class MemberTypeInfo {
        private final Class<?> returnType;
        private final Type genericReturnType;

        private MemberTypeInfo(Class<?> returnType, Type genericReturnType) {
            this.returnType = returnType;
            this.genericReturnType = genericReturnType;
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public Type getGenericReturnType() {
            return genericReturnType;
        }
    }
}
