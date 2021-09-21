package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonArrayDeserializer {
    private final ObjectReader reader;
    private final JsonParser parser;
    private final ByteArrayFeeder inputFeeder;
    private TokenBuffer tokenBuffer;
    private int depth = -1;

    public JsonArrayDeserializer(ObjectMapper objectMapper, Method method) {
        Type type = ReflectionUtil.getTypeOfObservable(method);

        JavaType javaType = TypeFactory.defaultInstance().constructType(type);
        reader   = objectMapper.readerFor(javaType);
        try {
            parser = objectMapper.getFactory().setCodec(objectMapper).createNonBlockingByteArrayParser();
            inputFeeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Publisher<?> process(byte[] buf) {
        try {
            inputFeeder.feedInput(buf, 0, buf.length);

            List<Object> items = null;
            Object item = null;
            JsonToken token;

            while ((token = parser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                if (tokenBuffer == null) {
                    tokenBuffer = new TokenBuffer(parser);
                    if (token == JsonToken.START_ARRAY && depth == -1) {
                        depth++;
                        continue;
                    }
                }
                if (item != null && items == null) {
                    items = new ArrayList<>();
                    items.add(item);
                }
                tokenBuffer.copyCurrentEvent(parser);
                if (token == JsonToken.START_ARRAY || token == JsonToken.START_OBJECT) {
                    depth++;
                } else if (token == JsonToken.END_ARRAY || token == JsonToken.END_OBJECT ) {
                    depth--;
                }
                if (depth == 0) {
                    item = reader.readValue(tokenBuffer.asParser());
                    if (item == null) {
                        continue;
                    }
                    tokenBuffer = null;
                    if (items != null) {
                        items.add(item);
                    }
                }
            }
            if (items != null) {
                return Flux.fromIterable(items);
            } else if (item != null) {
                return Flux.just(item);
            } else {
                return Flux.empty();
            }
        } catch (Exception e) {
            return Flux.error(e);
        }
    }
}
