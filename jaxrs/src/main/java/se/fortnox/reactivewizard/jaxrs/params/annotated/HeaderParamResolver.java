package se.fortnox.reactivewizard.jaxrs.params.annotated;

import com.fasterxml.jackson.core.type.TypeReference;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import java.lang.annotation.Annotation;

/**
 * Bind a header to a method parameter.
 */
class HeaderParamResolver<T> extends AnnotatedParamResolver<T> {

    public HeaderParamResolver(Deserializer<T> deserializer, Annotation headerParamAnnotation, String defaultValue) {
        super(deserializer, ((HeaderParam)headerParamAnnotation).value(), defaultValue);
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getHeader(parameterName, getDefaultValue());
    }

    public static class Factory implements AnnotatedParamResolverFactory {

        private final DeserializerFactory deserializerFactory;

        public Factory(DeserializerFactory deserializerFactory) {
            this.deserializerFactory = deserializerFactory;
        }

        @Override
        public <T> ParamResolver<T> create(TypeReference<T> paramType, Annotation annotation, String defaultValue) {
            return new HeaderParamResolver<>(deserializerFactory.getParamDeserializer(paramType), annotation, defaultValue);
        }
    }
}
