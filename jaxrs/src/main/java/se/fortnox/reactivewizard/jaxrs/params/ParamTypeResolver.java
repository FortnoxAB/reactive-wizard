package se.fortnox.reactivewizard.jaxrs.params;

import com.fasterxml.jackson.core.type.TypeReference;
import se.fortnox.reactivewizard.json.Types;

import java.lang.reflect.Parameter;

public class ParamTypeResolver {
    public <T> TypeReference<T> resolveParamType(Parameter instanceParameter, Parameter interfaceParameter) {
        return Types.toReference(interfaceParameter.getParameterizedType());
    }
}
