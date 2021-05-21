package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class JsonTokenBufferer {

    @Nonnull
    public static Flux<TokenBuffer> buffer(
        @Nonnull Flux<byte[]> byteFlux,
        @Nonnull ObjectMapper objectMapper
    ) {
        final JsonParser parser;
        try {
            //noinspection BlockingMethodInNonBlockingContext
            parser = objectMapper.getFactory().createNonBlockingByteArrayParser();
        } catch (IOException e) {
            return Flux.error(e);
        }

        final ByteArrayFeeder feeder = (ByteArrayFeeder)parser.getNonBlockingInputFeeder();

        final AtomicInteger structDepth = new AtomicInteger(0);

        final AtomicReference<TokenBuffer> currentBuffer = new AtomicReference<>(new TokenBuffer(parser));

        return byteFlux.flatMap(chunk -> Flux.fromStream(() -> {
            try {
                final List<TokenBuffer> completedBuffers = new ArrayList<>();

                feeder.feedInput(chunk, 0, chunk.length);

                while (parser.nextToken() != JsonToken.NOT_AVAILABLE) {
                    currentBuffer.get().copyCurrentEvent(parser);

                    if (parser.currentToken().isStructStart()) {
                        structDepth.incrementAndGet();
                    } else if (parser.currentToken().isStructEnd()) {
                        structDepth.decrementAndGet();
                    }

                    if (structDepth.get() == 0) {
                        completedBuffers.add(currentBuffer.get());
                        currentBuffer.set(new TokenBuffer(parser));
                    }
                }

                return completedBuffers.stream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
