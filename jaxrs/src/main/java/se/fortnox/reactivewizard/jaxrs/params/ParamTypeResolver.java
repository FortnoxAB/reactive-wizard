package se.fortnox.reactivewizard.jaxrs.params;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

public class ParamTypeResolver {
    public <T> TypeReference<T> resolveParamType(Parameter instanceParameter, Parameter interfaceParameter) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return interfaceParameter.getParameterizedType();
            }
        };
    }
}
