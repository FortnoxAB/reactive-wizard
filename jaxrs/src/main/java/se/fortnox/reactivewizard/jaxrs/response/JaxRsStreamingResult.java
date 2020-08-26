package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServerResponse;
import rx.functions.Func1;

import java.util.Map;

public class JaxRsStreamingResult<T> extends JaxRsResult<T> {
    public JaxRsStreamingResult(Flux<T> output, HttpResponseStatus responseStatus, Func1<Flux<T>, Flux<byte[]>> serializer, Map<String, String> headers) {
        super(output, responseStatus, serializer, headers);
    }

    @Override
    public Publisher<Void> write(HttpServerResponse response) {
        return output.switchOnFirst((s, outputBuffered)->{
            if (s.isOnError()) {
                return Flux.error(s.getThrowable());
            }
            response.status(responseStatus);
            headers.forEach(response::addHeader);
            return response.sendByteArray(serializer.call(outputBuffered));
        });
    }
}
