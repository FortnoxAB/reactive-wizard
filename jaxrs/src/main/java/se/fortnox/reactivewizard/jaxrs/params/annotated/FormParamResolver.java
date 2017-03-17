package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;

import javax.ws.rs.FormParam;
import java.lang.annotation.Annotation;

class FormParamResolver<T> extends AnnotatedParamResolver<T> {

    public FormParamResolver(Annotation fromParamAnnotation, Deserializer<T> deserializer) {
        super(deserializer, ((FormParam)fromParamAnnotation).value());
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getFormParam(paramName);
    }
}
