package se.fortnox.reactivewizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static java.util.Objects.isNull;

/**
 * Wrapper of Dropwizard metrics framework for use with observers.
 */
public class Metrics {
    private static final int            NANOSECONDS_PER_MILLISECOND = 1000000;
    private static final MetricRegistry REGISTRY = new MetricRegistry();
    private static final LongConsumer NOOP     = whatever -> {
    };

    private final Timer timer;

    private Metrics(String name) {
        this.timer = REGISTRY.timer(name);
    }

    public static Metrics get(String name) {
        return new Metrics(name);
    }

    public static MetricRegistry registry() {
        return REGISTRY;
    }

    public <T> Publisher<T> measure(Publisher<T> publisher) {
        return measure(publisher, NOOP);
    }

    public <T> Mono<T> measure(Mono<T> publisher) {
        if (isNull(publisher)) {
            return null;
        }

        return Mono.from(measure(publisher, NOOP));
    }

    public <T> Flux<T> measure(Flux<T> publisher) {
        if (isNull(publisher)) {
            return null;
        }

        return Flux.from(measure(publisher, NOOP));
    }

    /**
     * Measure the execution time of the Publisher.
     * @param publisher Publisher to measure execution time on.
     * @param callback Callback given the execution time to run when publisher terminates.
     * @param <T> any type parameter.
     * @return Publisher that also measures its execution time.
     */
    public <T> Publisher<T> measure(Publisher<T> publisher, LongConsumer callback) {
        if (isNull(publisher)) {
            return null;
        }

        return Flux.defer(() -> {
            Timer.Context context = timer.time();
            return Flux.from(publisher).doOnTerminate(() -> callback.accept(getElapsedTime(context)));
        });
    }

    private long getElapsedTime(Timer.Context context) {
        return context.stop() / NANOSECONDS_PER_MILLISECOND;
    }
}
