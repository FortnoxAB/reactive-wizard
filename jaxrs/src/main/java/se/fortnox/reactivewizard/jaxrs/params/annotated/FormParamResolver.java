package se.fortnox.reactivewizard.jaxrs.params.annotated;

import com.fasterxml.jackson.core.type.TypeReference;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import java.lang.annotation.Annotation;

/**
 * Bind a form field to a method parameter.
 */
class FormParamResolver<T> extends AnnotatedParamResolver<T> {

    public FormParamResolver(Deserializer<T> deserializer, Annotation fromParamAnnotation, String defaultValue) {
        super(deserializer, ((FormParam)fromParamAnnotation).value(), defaultValue);
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getFormParam(parameterName, getDefaultValue());
    }

    public static class Factory implements AnnotatedParamResolverFactory {

        private final DeserializerFactory deserializerFactory;

        public Factory(DeserializerFactory deserializerFactory) {
            this.deserializerFactory = deserializerFactory;
        }

        @Override
        public <T> ParamResolver<T> create(TypeReference<T> paramType, Annotation annotation, String defaultValue) {
            return new FormParamResolver<>(deserializerFactory.getParamDeserializer(paramType), annotation, defaultValue);
        }
    }
}
