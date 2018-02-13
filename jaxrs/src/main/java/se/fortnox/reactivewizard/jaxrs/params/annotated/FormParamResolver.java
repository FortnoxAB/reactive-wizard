package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import java.lang.annotation.Annotation;

/**
 * Bind a form field to a method parameter.
 */
class FormParamResolver<T> extends AnnotatedParamResolver<T> {

    public FormParamResolver(Annotation fromParamAnnotation, Deserializer<T> deserializer, DefaultValue defaultValueAnnotation) {
        super(deserializer, ((FormParam)fromParamAnnotation).value(), defaultValueAnnotation);
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getFormParam(parameterName, getDefaultValue());
    }
}
