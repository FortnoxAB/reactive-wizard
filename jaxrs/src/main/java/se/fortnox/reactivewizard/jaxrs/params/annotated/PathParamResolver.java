package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import java.lang.annotation.Annotation;

class PathParamResolver<T> extends AnnotatedParamResolver<T> {

    public PathParamResolver(Annotation pathParamAnnotation, Deserializer<T> deserializer, DefaultValue defaultValueAnnotation) {
        super(deserializer, ((PathParam)pathParamAnnotation).value(), defaultValueAnnotation);
        if (defaultValueAnnotation != null) {
            throw new UnsupportedOperationException("@DefaultValue is not implemented for @PathParam");
        }
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getPathParam(paramName, getDefaultValue());
    }
}
