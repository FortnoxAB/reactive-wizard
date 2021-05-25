package se.fortnox.reactivewizard.client;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Internal class to help support getting the full response as return value when observable is returned
 * @param <T> the type of data to be returned
 */
class FluxWithResponse<T> extends Flux<T> {

    private final Mono<Response<Flux<T>>> inner;

    public FluxWithResponse(Mono<Response<Flux<T>>> inner) {
        this.inner = inner;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> coreSubscriber) {
        inner.flatMapMany(Response::getBody).subscribe(coreSubscriber);
    }

    public Mono<Response<Flux<T>>> getResponse() {
        return inner;
    }
}
