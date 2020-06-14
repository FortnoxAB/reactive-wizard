package se.fortnox.reactivewizard.util;

import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import rx.Observable;

public class FluxRxConverter {

    public static <T> Flux<T> observableToFlux(Observable<T> result) {
        return Flux.from(subscriber -> result.subscribe(new FluxRxSubscriber<T>(subscriber)));
    }

    private static class FluxRxSubscriber<T> extends rx.Subscriber<T> {

        private final org.reactivestreams.Subscriber<? super T> subscriber;

        public FluxRxSubscriber(org.reactivestreams.Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onStart() {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long count) {
                    FluxRxSubscriber.this.request(count);
                }

                @Override
                public void cancel() {
                    FluxRxSubscriber.this.unsubscribe();
                }
            });
        }

        @Override
        public void onCompleted() {
            subscriber.onComplete();
        }

        @Override
        public void onError(Throwable error) {
            subscriber.onError(error);
        }

        @Override
        public void onNext(T item) {
            subscriber.onNext(item);
        }

    }

}
