package se.fortnox.reactivewizard.reactorclient;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.reactivestreams.Publisher;
import se.fortnox.reactivewizard.metrics.Metrics;

import java.util.function.Consumer;

/**
 * Wrapper of Dropwizard metrics framework for use with observers.
 */
public class PublisherMetrics {
    private static final MetricRegistry REGISTRY = Metrics.registry();
    private static final Consumer<Long> NOOP     = whatever -> {
    };

    private Timer timer;

    private PublisherMetrics(String name) {
        this.timer = REGISTRY.timer(name);
    }

    public static PublisherMetrics get(String name) {
        return new PublisherMetrics(name);
    }

    public <T extends Publisher<S>, S> T measure(T publisher) {
        return measure(publisher, NOOP);
    }

    public <T extends Publisher<S>, S> T measure(T publisher, Consumer<Long> callback) {
        return (T)new MetricsOperator<S>(publisher, timer, callback);
    }
}
