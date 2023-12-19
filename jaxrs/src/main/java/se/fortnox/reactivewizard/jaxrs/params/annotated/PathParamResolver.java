package se.fortnox.reactivewizard.jaxrs.params.annotated;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.PathParam;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;

import java.lang.annotation.Annotation;

/**
 * Bind a path parameter to a method parameter.
 */
class PathParamResolver<T> extends AnnotatedParamResolver<T> {

    public PathParamResolver(Deserializer<T> deserializer, Annotation pathParamAnnotation, String defaultValue) {
        super(deserializer, ((PathParam)pathParamAnnotation).value(), defaultValue);
        if (defaultValue != null) {
            throw new UnsupportedOperationException("@DefaultValue is not implemented for @PathParam");
        }
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getPathParam(parameterName, getDefaultValue());
    }

    public static class Factory implements AnnotatedParamResolverFactory {

        private final DeserializerFactory deserializerFactory;

        public Factory(DeserializerFactory deserializerFactory) {
            this.deserializerFactory = deserializerFactory;
        }

        @Override
        public <T> ParamResolver<T> create(TypeReference<T> paramType, Annotation annotation, String defaultValue) {
            return new PathParamResolver<>(deserializerFactory.getParamDeserializer(paramType), annotation, defaultValue);
        }
    }

}
