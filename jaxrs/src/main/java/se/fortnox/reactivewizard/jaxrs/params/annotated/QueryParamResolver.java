package se.fortnox.reactivewizard.jaxrs.params.annotated;

import com.fasterxml.jackson.core.type.TypeReference;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;

import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;

/**
 * Bind a query parameter to a method parameter.
 */
public class QueryParamResolver<T> extends AnnotatedParamResolver<T> {

    public QueryParamResolver(Deserializer<T> deserializer, Annotation queryParamAnnotation, String defaultValue) {
        super(deserializer, ((QueryParam)queryParamAnnotation).value(), defaultValue);
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getQueryParam(parameterName, getDefaultValue());
    }

    public static class Factory implements AnnotatedParamResolverFactory {

        private final DeserializerFactory deserializerFactory;

        public Factory(DeserializerFactory deserializerFactory) {
            this.deserializerFactory = deserializerFactory;
        }

        @Override
        public <T> ParamResolver<T> create(TypeReference<T> paramType, Annotation annotation, String defaultValue) {
            return new QueryParamResolver<>(deserializerFactory.getParamDeserializer(paramType), annotation, defaultValue);
        }
    }
}
