package se.fortnox.reactivewizard.jaxrs.params;

import com.fasterxml.jackson.core.type.TypeReference;
import se.fortnox.reactivewizard.jaxrs.Wrap;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

public class WrapSupportingParamTypeResolver extends ParamTypeResolver {

    @Override
    public <T> TypeReference<T> resolveParamType(Parameter instanceParameter, Parameter interfaceParameter) {
        Wrap wrapAnnotation = instanceParameter.getAnnotation(Wrap.class);
        if (wrapAnnotation != null) {
            Class<?> wrapperType = wrapAnnotation.value();
            if (!instanceParameter.getType().isAssignableFrom(wrapperType)) {
                throw new RuntimeException("Wrapper for " + instanceParameter.getDeclaringExecutable() + " not correct. "
                    + wrapperType + " must be subclass of " + instanceParameter.getType());
            }
            return new TypeReference<T>() {
                @Override
                public Type getType() {
                    return wrapperType;
                }
            };
        }
        return super.resolveParamType(instanceParameter, interfaceParameter);
    }
}
