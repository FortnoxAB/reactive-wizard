package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServerResponse;
import rx.functions.Func1;

import java.util.Map;

public class JaxRsStreamingResult<T> extends JaxRsResult<T> {
    public JaxRsStreamingResult(Flux<T> output, HttpResponseStatus responseStatus, Func1<T, byte[]> serializer, Map<String, String> headers) {
        super(output, responseStatus, serializer, headers);
    }

    @Override
    public Publisher<Void> write(HttpServerResponse response) {
        response.status(responseStatus);
        headers.forEach(response::addHeader);
        return response.sendByteArray(output.map(serializer::call));
    }
}
