package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

/**
 * Represents a result of a call to a JaxRs resource. Contains the output but also some meta data about the call.
 * This class is passed to the output processors.
 */
public class JaxRsResult<T> {
    protected static final byte[] EMPTY_RESPONSE = new byte[0];

    protected final Func1<Flux<T>, Flux<byte[]>>    serializer;
    protected final Map<String, String> headers = new HashMap<>();
    protected       Flux<T>       output;
    protected       HttpResponseStatus  responseStatus;

    public JaxRsResult(Flux<T> output, HttpResponseStatus responseStatus, Func1<Flux<T>, Flux<byte[]>> serializer, Map<String, String> headers) {
        this.output = output;
        this.responseStatus = responseStatus;
        this.serializer = serializer;
        this.headers.putAll(headers);
    }

    public HttpResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public JaxRsResult<T> addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public JaxRsResult<T> doOnOutput(Action1<T> action) {
        output = output.doOnNext(action::call);
        return this;
    }

    public JaxRsResult<T> map(Func1<Flux<T>, Flux<T>> mapFunction) {
        output = mapFunction.call(output);
        return this;
    }

    public Publisher<Void> write(HttpServerResponse response) {
        AtomicBoolean headersWritten = new AtomicBoolean();
        return serializer.call(output)
            .switchIfEmpty(Flux.defer(() -> {
                if (responseStatus.codeClass() == HttpStatusClass.SUCCESS) {
                    responseStatus = HttpResponseStatus.NO_CONTENT;
                }
                response.status(responseStatus);
                response.addHeader(CONTENT_LENGTH, "0");
                return Flux.empty();
            }))
            .flatMap(bytes -> {
                int contentLength = getContentLength(bytes);

                if (headersWritten.compareAndSet(false, true)) {
                    response.status(responseStatus);
                    headers.forEach(response::addHeader);
                    response.addHeader(CONTENT_LENGTH, String.valueOf(contentLength));
                }

                if (contentLength > 0) {
                    return response.sendByteArray(Mono.just(bytes));
                }

                return Flux.empty();
            });
    }

    private int getContentLength(byte[] bytes) {
        return bytes != null ? bytes.length : 0;
    }
}
