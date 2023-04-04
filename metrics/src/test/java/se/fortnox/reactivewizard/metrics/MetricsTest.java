package se.fortnox.reactivewizard.metrics;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTest {

    @Test
    public void shouldMeasureFlux() {
        String metricName = "test1";
        Flux<Integer> measuredFlux = Metrics.get(metricName)
            .measure(Flux.just(1));
        measuredFlux.subscribe();

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(1);
    }

    @Test
    public void shouldStopMeasuringOnError() {
        String metricName = "test2";
        Flux<Integer> measuredFlux = Metrics.get(metricName)
            .measure(Flux.error(new Exception()));
        StepVerifier.create(measuredFlux).verifyError(Exception.class);

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(1);
    }

    @Test
    public void shouldHandleNullFluxes() {
        String metricName = "test3";
        Flux<Integer> measuredFlux = Metrics.get(metricName)
            .measure((Flux<Integer>)null);

        assertThat(measuredFlux).isNull();
    }

    @Test
    public void shouldHandleMultipleCallsToMeasure() {
        String              metricName          = "test4";
        Metrics             metrics             = Metrics.get(metricName);
        Flux<Integer> measuredFlux1 = metrics.measure(Flux.just(1));
        measuredFlux1.subscribe();
        measuredFlux1.subscribe();
        measuredFlux1.subscribe();
        Flux<Integer> measuredFlux2 = metrics.measure(Flux.just(1));
        measuredFlux2.subscribe();
        measuredFlux2.subscribe();

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(5);
    }
}
