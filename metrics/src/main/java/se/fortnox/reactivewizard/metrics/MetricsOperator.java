package se.fortnox.reactivewizard.metrics;

import com.codahale.metrics.Timer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public class MetricsOperator<T> extends Flux<T> {

    private static final int            NS_TO_MS = 1000000;
    private final        Publisher<T>   source;
    private              Timer          timer;
    private              Consumer<Long> callback;

    public MetricsOperator(Publisher<T> source, Timer timer,  Consumer<Long> callback) {
        super();
        this.source = source;
        this.timer = timer;
        this.callback = callback;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> coreSubscriber) {
        final Subscriber<T> wrappedSubscriber = createSubscriber(coreSubscriber, timer);
        source.subscribe(wrappedSubscriber);
    }

    private Subscriber<T> createSubscriber(Subscriber<? super T> target, Timer timer) {
        return new Subscriber<T>() {
            private Subscription subscription;
            private Timer.Context context;
            @Override
            public void onSubscribe(Subscription subscription) {
                context = timer.time();
                this.subscription = subscription;
                target.onSubscribe(subscription);
                this.subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T next) {
                if (next != null) {
                    target.onNext(next);
                }
                this.subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onError(Throwable throwable) {
                callback.accept(context.stop() / NS_TO_MS);
                target.onError(throwable);
            }

            @Override
            public void onComplete() {
                callback.accept(context.stop() / NS_TO_MS);
                target.onComplete();
            }
        };
    }
}
