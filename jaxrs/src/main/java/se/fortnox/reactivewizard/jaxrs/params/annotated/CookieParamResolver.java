package se.fortnox.reactivewizard.jaxrs.params.annotated;

import com.fasterxml.jackson.core.type.TypeReference;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;

import javax.ws.rs.CookieParam;
import java.lang.annotation.Annotation;

/**
 * Bind a cookie to a method parameter.
 */
public class CookieParamResolver<T> extends AnnotatedParamResolver<T> {

    public CookieParamResolver(Deserializer<T> deserializer, Annotation cookieParamAnnotation, String defaultValue) {
        super(deserializer, ((CookieParam)cookieParamAnnotation).value(), defaultValue);
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return request.getCookieValue(parameterName, getDefaultValue());
    }

    public static class Factory implements AnnotatedParamResolverFactory {

        private final DeserializerFactory deserializerFactory;

        public Factory(DeserializerFactory deserializerFactory) {
            this.deserializerFactory = deserializerFactory;
        }

        @Override
        public <T> ParamResolver<T> create(TypeReference<T> paramType, Annotation annotation, String defaultValue) {
            return new CookieParamResolver<>(deserializerFactory.getParamDeserializer(paramType), annotation, defaultValue);
        }
    }
}
