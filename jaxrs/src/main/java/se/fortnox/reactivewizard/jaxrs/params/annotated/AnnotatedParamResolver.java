package se.fortnox.reactivewizard.jaxrs.params.annotated;

import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.jaxrs.FieldError;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerException;

abstract class AnnotatedParamResolver<T> implements ParamResolver<T> {

    protected final String          parameterName;
    private final   Deserializer<T> deserializer;
    private final   String          defaultValue;

    public AnnotatedParamResolver(Deserializer<T> deserializer, String parameterName, String defaultValue) {
        this.deserializer = deserializer;
        this.parameterName = parameterName;
        this.defaultValue = defaultValue;
    }

    protected abstract String getValue(JaxRsRequest request);

    protected String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Mono<T> resolve(JaxRsRequest request) {
        try {
            return Mono.justOrEmpty(deserializer.deserialize(getValue(request)));
        } catch (DeserializerException deserializerException) {
            throw new WebException(HttpResponseStatus.BAD_REQUEST, new FieldError(parameterName, deserializerException.getMessage()));
        }
    }
}

