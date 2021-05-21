package se.fortnox.reactivewizard.client;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClientResponse;
import rx.Observable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Internal class to help support getting the full response as return value when observable is returned
 * @param <T> the type of data to be returned
 */
class FluxWithResponse<T> extends Flux<T> {

    private final Flux<T> inner;
    private final AtomicReference<HttpClientResponse> httpClientResponse;

    FluxWithResponse(Flux<T> inner, AtomicReference<HttpClientResponse> httpClientResponse) {
        this.inner = inner;
        this.httpClientResponse = httpClientResponse;
    }

    HttpClientResponse getResponse() {
        if (httpClientResponse.get() == null) {
            throw new IllegalStateException("This method can only be called after the response has been received");
        }
        return httpClientResponse.get();
    }

    static <T> FluxWithResponse<T> from(FluxWithResponse<T> observableWithResponse, Flux<T> inner) {
        return new FluxWithResponse<T>(inner, observableWithResponse.httpClientResponse);
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> coreSubscriber) {
        inner.subscribe(coreSubscriber);
    }
}
