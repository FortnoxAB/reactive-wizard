package se.fortnox.reactivewizard.jaxrs.response;

import jakarta.inject.Inject;
import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.jaxrs.Stream;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;

import javax.ws.rs.core.MediaType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static reactor.core.publisher.Flux.just;

public class JaxRsResultSerializerFactory {
    private static final Class<byte[]>   BYTEARRAY_TYPE = byte[].class;
    private static final Charset         charset        = StandardCharsets.UTF_8;
    private static final byte[] JSON_ARRAY_START = "[".getBytes();
    private static final byte[] JSON_ARRAY_END = "]".getBytes();

    private static final byte[] COMMA = ",".getBytes();

    private final JsonSerializerFactory jsonSerializerFactory;


    @Inject
    public JaxRsResultSerializerFactory(JsonSerializerFactory jsonSerializerFactory) {
        this.jsonSerializerFactory = jsonSerializerFactory;
    }

    /**
     * Creates a non streaming result serializer
     * @param type Content-Type
     * @param dataCls Return type of the resource method
     * @param returnTypeIsFlux True if it is a flux, false otherwise
     * @param <T> Type of elements emitted by the publisher
     * @return a function that serializes the elements emitted by a publisher to a Flux of byte arrays.
     */
    public <T> Function<Flux<T>, Flux<byte[]>> createSerializer(String type, Class<T> dataCls, boolean returnTypeIsFlux) {

        if (type.equals(MediaType.APPLICATION_JSON)) {
            if (!returnTypeIsFlux) {
                var byteSerializer = jsonSerializerFactory.createByteSerializer(dataCls);
                return serializedItems -> serializedItems.map(byteSerializer);
            } else {
                var listToByteSerializer = jsonSerializerFactory.createListToByteSerializer(dataCls);
                return serializedItems -> serializedItems.buffer().defaultIfEmpty(emptyList()).map(listToByteSerializer);
            }
        }
        if (dataCls.equals(BYTEARRAY_TYPE)) {
            return serializedItems -> serializedItems.cast(byte[].class);
        }

        if (returnTypeIsFlux) {
            return serializedItems -> serializedItems.map(Object::toString).reduce(String::concat).map(String::getBytes).flux();
        }
        return serializedItems -> serializedItems.map(object -> object.toString().getBytes());
    }


    /**
     * Creates a streaming result serializer.
     * @param type Content-Type
     * @param dataCls Return type of the resource method
     * @param streamType Stream type
     * @param returnTypeIsFlux True if it is a flux, false otherwise
     * @param <T> Type of elements emitted by the publisher
     * @return a function that serializes the elements emitted by a publisher to a Flux of byte arrays.
     */
    public <T> Function<Flux<T>, Flux<byte[]>> createStreamingSerializer(String type, Class<T> dataCls, Stream.Type streamType, boolean returnTypeIsFlux) {
        if (type.equals(MediaType.APPLICATION_JSON)) {
            var byteSerializer = jsonSerializerFactory.createByteSerializer(dataCls);
            if (streamType == Stream.Type.CONCATENATED_JSON_OBJECTS || !returnTypeIsFlux) {
                return serializedItems -> serializedItems.map(byteSerializer);
            } else {
                return serializedItems -> {
                    AtomicBoolean first = new AtomicBoolean(true);
                    Flux<byte[]> items = serializedItems.concatMap(item -> {
                        if (first.getAndSet(false)) {
                            return just(byteSerializer.apply(item));
                        } else {
                            return just(COMMA, byteSerializer.apply(item));
                        }
                    });
                    return Flux.concat(
                        just(JSON_ARRAY_START),
                        items,
                        just(JSON_ARRAY_END));
                };
            }
        }
        if (dataCls.equals(BYTEARRAY_TYPE)) {
            return flux -> flux.map(byte[].class::cast);
        }
        return flux -> flux.map(data -> data.toString().getBytes(charset));
    }
}
