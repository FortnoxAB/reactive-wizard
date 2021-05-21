package se.fortnox.reactivewizard.client;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientResponse;

import java.util.concurrent.atomic.AtomicReference;

public class MonoWithResponse<T> extends Mono<T> {
    private final Mono<T> inner;
    private final AtomicReference<HttpClientResponse> rawResponse;

    public MonoWithResponse(Mono<T> inner, AtomicReference<HttpClientResponse> rawResponse) {
        this.inner = inner;
        this.rawResponse = rawResponse;
    }

    HttpClientResponse getResponse() {
        if (rawResponse.get() == null) {
            throw new IllegalStateException("This method can only be called after the response has been received");
        }
        return rawResponse.get();
    }

    static <T> MonoWithResponse<T> from(MonoWithResponse<T> monoWithResponse, Mono<T> inner) {
        return new MonoWithResponse<T>(inner, monoWithResponse.rawResponse);
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> coreSubscriber) {
        inner.subscribe(coreSubscriber);
    }
}
