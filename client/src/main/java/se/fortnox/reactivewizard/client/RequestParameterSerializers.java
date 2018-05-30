package se.fortnox.reactivewizard.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Keeps tracks of a map from class to correct RequestParameterSerializer
 */
public class RequestParameterSerializers {

    private final Map<Class, RequestParameterSerializer> serializers;


    @Inject
    public RequestParameterSerializers(Set<RequestParameterSerializer> requestParameterSerializers) {
        serializers = requestParameterSerializers.stream().collect(Collectors.toMap(this::getAppliedClass, Function.identity()));
    }

    private Class<?> getAppliedClass(RequestParameterSerializer serializer) {
        Optional<? extends Class<?>> cls = Arrays.stream(serializer.getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals("addParameter"))
                .map(m -> m.getParameterTypes()[0])
                .findFirst();

        if (!cls.isPresent()) {
            throw new RuntimeException(cls+" is missing addParameter");
        }

        return cls.get();
    }

    public RequestParameterSerializers() {
        serializers = Collections.emptyMap();
    }

    public RequestParameterSerializer<?> getSerializer(Class<?> type) {
        return serializers.get(type);
    }
}
