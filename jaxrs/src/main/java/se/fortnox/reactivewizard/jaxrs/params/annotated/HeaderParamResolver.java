package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import java.lang.annotation.Annotation;

/**
 * Bind a header to a method parameter.
 */
class HeaderParamResolver<T> extends AnnotatedParamResolver<T> {

    public HeaderParamResolver(Annotation headerParamAnnotation, Deserializer<T> deserializer, DefaultValue defaultValueAnnotation) {
        super(deserializer, ((HeaderParam)headerParamAnnotation).value(), defaultValueAnnotation);
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getHeader(parameterName, getDefaultValue());
    }
}
