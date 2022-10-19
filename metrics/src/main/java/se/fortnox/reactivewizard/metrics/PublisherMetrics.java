package se.fortnox.reactivewizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * Wrapper of Dropwizard metrics framework for use with observers.
 */
public class PublisherMetrics {
    private static final MetricRegistry REGISTRY = Metrics.registry();
    private static final Consumer<Long> NOOP     = whatever -> {
    };

    private final Timer timer;

    private PublisherMetrics(String name) {
        this.timer = REGISTRY.timer(name);
    }

    public static PublisherMetrics get(String name) {
        return new PublisherMetrics(name);
    }

    public <T> Publisher<T> measure(Publisher<T> publisher) {
        return measure(publisher, NOOP);
    }

    public <T> Mono<T> measure(Mono<T> publisher) {
        return Mono.from(measure(publisher, NOOP));
    }

    public <T> Flux<T> measure(Flux<T> publisher) {
        return Flux.from(measure(publisher, NOOP));
    }

    public <T> Publisher<T> measure(Publisher<T> publisher, Consumer<Long> callback) {
        return new MetricsOperator<>(publisher, timer, callback);
    }
}
