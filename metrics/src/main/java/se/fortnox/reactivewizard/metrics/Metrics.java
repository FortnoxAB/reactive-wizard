package se.fortnox.reactivewizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;

import java.util.function.Consumer;

/**
 * Wrapper of Dropwizard metrics framework for use with observers.
 */
public class Metrics {
    private static final int            NS_TO_MS = 1000000;
    private static final MetricRegistry REGISTRY = new MetricRegistry();
    private static final Consumer<Long> NOOP     = whatever -> {
    };

    private Timer timer;

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

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public <T> Observable<T> measure(Observable<T> observable, Consumer<Long> callback) {
        if (observable == null) {
            return null;
        }
        return observable.lift(new Operator<T, T>() {
            @Override
            public Subscriber<? super T> call(Subscriber<? super T> subscriber) {
                Context context = timer.time();
                return new Subscriber<T>(subscriber) {

                    @Override
                    public void onCompleted() {
                        callback.accept(context.stop() / NS_TO_MS);
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        callback.accept(context.stop() / NS_TO_MS);
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
            }
        });
    }

}
