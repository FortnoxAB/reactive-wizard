package se.fortnox.reactivewizard.client;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
        Optional<? extends Class<?>> cls = Arrays.stream(serializer.getClass().getDeclaredMethods())
            .filter(method -> method.getName().equals("addParameter"))
            .map(method -> method.getParameterTypes()[0])
            .findFirst();

        if (!cls.isPresent()) {
            throw new RuntimeException(cls + " is missing addParameter");
        }

        return cls.get();
    }

    public RequestParameterSerializer<?> getSerializer(Class<?> type) {
        return serializers.get(type);
    }
}
