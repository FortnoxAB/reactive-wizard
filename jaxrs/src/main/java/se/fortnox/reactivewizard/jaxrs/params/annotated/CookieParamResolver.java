package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;

import javax.ws.rs.CookieParam;
import java.lang.annotation.Annotation;

public class CookieParamResolver<T> extends AnnotatedParamResolver<T> {

    public CookieParamResolver(Annotation cookieParamAnnotation, Deserializer<T> deserializer) {
        super(deserializer, ((CookieParam)cookieParamAnnotation).value());
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getCookieValue(paramName);
    }
}
