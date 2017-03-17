package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;

import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;

class QueryParamResolver<T> extends AnnotatedParamResolver<T> {

    public QueryParamResolver(Annotation queryParamAnnotation, Deserializer<T> deserializer) {
        super(deserializer, ((QueryParam)queryParamAnnotation).value());
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getQueryParam(paramName);
    }
}
