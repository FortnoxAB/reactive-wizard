package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import java.lang.annotation.Annotation;

/**
 * Bind a cookie to a method parameter.
 */
public class CookieParamResolver<T> extends AnnotatedParamResolver<T> {

    public CookieParamResolver(Annotation cookieParamAnnotation, Deserializer<T> deserializer, DefaultValue defaultValueAnnotation) {
        super(deserializer, ((CookieParam)cookieParamAnnotation).value(), defaultValueAnnotation);
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getCookieValue(parameterName, getDefaultValue());
    }
}
