package se.fortnox.reactivewizard.metrics;

import com.codahale.metrics.Timer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.util.annotation.NonNull;

import java.util.function.Consumer;

public class MetricsOperator<T> implements Publisher<T> {

    private static final int            NS_TO_MS = 1000000;
    private final        Publisher<T>   source;
    private              Timer          timer;
    private              Consumer<Long> callback;

    public MetricsOperator(Publisher<T> source, Timer timer, Consumer<Long> callback) {
        super();
        this.source = source;
        this.timer = timer;
        this.callback = callback;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        final Subscriber<T> wrappedSubscriber = createSubscriber(subscriber, timer);
        source.subscribe(wrappedSubscriber);
    }

    private Subscriber<T> createSubscriber(Subscriber<? super T> target, Timer timer) {
        return new Subscriber<>() {
            private Timer.Context context;

            @Override
            public void onSubscribe(Subscription subscription) {
                context = timer.time();
                target.onSubscribe(subscription);
            }

            @Override
            public void onNext(@NonNull T next) {
                target.onNext(next);
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
