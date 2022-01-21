package se.fortnox.reactivewizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import rx.Observable;
import rx.Subscriber;

import java.util.function.LongConsumer;

/**
 * Wrapper of Dropwizard metrics framework for use with observers.
 */
public class Metrics {
    private static final int            NANOSECONDS_PER_MILLISECOND = 1000000;
    private static final MetricRegistry REGISTRY                    = new MetricRegistry();
    private static final LongConsumer   NOOP                        = whatever -> {
    };

    private final Timer timer;

    private Metrics(String name) {
        this.timer = REGISTRY.timer(name);
    }

    public static MetricRegistry registry() {
        return REGISTRY;
    }

    public static Metrics get(String name) {
        return new Metrics(name);
    }

    public <T> Observable<T> measure(Observable<T> observable) {
        return measure(observable, NOOP);
    }

    /**
     * Measure execution time of an Observable.
     * @param observable the Observable
     * @param callback the callback
     * @param <T> the type of observable
     * @return an Observable with measure applied
     */
    public <T> Observable<T> measure(Observable<T> observable, LongConsumer callback) {
        if (observable == null) {
            return null;
        }
        return observable.lift(subscriber -> {
            Context context = timer.time();
            return new Subscriber<T>(subscriber) {
                @Override
                public void onCompleted() {
                    callback.accept(context.stop() / NANOSECONDS_PER_MILLISECOND);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    callback.accept(context.stop() / NANOSECONDS_PER_MILLISECOND);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(throwable);
                    }
                }

                @Override
                public void onNext(T type) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(type);
                    }
                }
            };
        });
    }
}
