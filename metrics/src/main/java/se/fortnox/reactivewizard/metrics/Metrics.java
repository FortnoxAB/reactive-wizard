package se.fortnox.reactivewizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import reactor.core.publisher.Flux;

import java.util.function.LongConsumer;

import static java.util.Objects.isNull;

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

    public <T> Flux<T> measure(Flux<T> anyFlux) {
        return measure(anyFlux, NOOP);
    }

    /**
     * Measure execution time of Flux.
     * @param anyFlux the flux
     * @param consumer the callback
     * @param <T> The type parameter of the returned flux
     * @return Flux that is measuring the time until termination
     */
    public <T> Flux<T> measure(Flux<T> anyFlux, LongConsumer consumer) {
        if (isNull(anyFlux)) {
            return null;
        }

        return Flux.defer(() -> {
            Context context = timer.time();
            return anyFlux.doOnTerminate(() -> consumer.accept(getElapsedTime(context)));
        });
    }

    private long getElapsedTime(Context context) {
        return context.stop() / NANOSECONDS_PER_MILLISECOND;
    }
}
