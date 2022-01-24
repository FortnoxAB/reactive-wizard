package se.fortnox.reactivewizard.client;

import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps tracks of a map from class to correct RequestParameterSerializer.
 */
public class RequestParameterSerializers {

    private final Map<Class, RequestParameterSerializer> serializers;

    @Inject
    public RequestParameterSerializers(Set<RequestParameterSerializer> requestParameterSerializers) {
        serializers = requestParameterSerializers.stream().collect(Collectors.toMap(this::getAppliedClass, Function.identity()));
    }

    public RequestParameterSerializers() {
        serializers = Collections.emptyMap();
    }

    private Class<?> getAppliedClass(RequestParameterSerializer serializer) {
        return (Class)((ParameterizedType)serializer.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }

    public RequestParameterSerializer<?> getSerializer(Class<?> type) {
        return serializers.get(type);
    }
}
