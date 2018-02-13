package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;

/**
 * Bind a query parameter to a method parameter.
 */
class QueryParamResolver<T> extends AnnotatedParamResolver<T> {

    public QueryParamResolver(Annotation queryParamAnnotation, Deserializer<T> deserializer, DefaultValue defaultValueAnnotation) {
        super(deserializer, ((QueryParam)queryParamAnnotation).value(), defaultValueAnnotation);
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getQueryParam(parameterName, getDefaultValue());
    }
}
