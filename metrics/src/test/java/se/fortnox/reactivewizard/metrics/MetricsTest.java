package se.fortnox.reactivewizard.metrics;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsTest {

    @Test
    void shouldMeasureFlux() {
        String metricName = "test1_flux";
        Flux<Integer> measuredFlux = Metrics.get(metricName)
            .measure(Flux.just(1));
        measuredFlux.subscribe();

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(1);
    }

    @Test
    void shouldMeasureMono() {
        String metricName = "test1_mono";
        Mono<Integer> measuredMono = Metrics.get(metricName)
            .measure(Mono.just(1));
        measuredMono.subscribe();

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(1);
    }

    @Test
    void shouldStopMeasuringOnErrorMono() {
        String metricName = "test2_mono";
        Mono<Integer> measuredMono = Metrics.get(metricName)
            .measure(Mono.error(new Exception()));
        StepVerifier.create(measuredMono).verifyError(Exception.class);

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(1);
    }

    @Test
    void shouldStopMeasuringOnError() {
        String metricName = "test2_flux";
        Flux<Integer> measuredFlux = Metrics.get(metricName)
            .measure(Flux.error(new Exception()));
        StepVerifier.create(measuredFlux).verifyError(Exception.class);

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(1);
    }

    @Test
    void shouldHandleNullFluxes() {
        String metricName = "test3_flux";
        Flux<Integer> measuredFlux = Metrics.get(metricName)
            .measure((Flux<Integer>)null);

        assertThat(measuredFlux).isNull();
    }

    @Test
    void shouldHandleNullMonos() {
        String metricName = "test3_mono";
        Mono<Integer> measuredMono = Metrics.get(metricName)
            .measure((Mono<Integer>)null);

        assertThat(measuredMono).isNull();
    }

    @Test
    void shouldHandleMultipleCallsToMeasureFlux() {
        String              metricName          = "test4_flux";
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

    @Test
    void shouldHandleMultipleCallsToMeasureMono() {
        String              metricName          = "test4_mono";
        Metrics             metrics             = Metrics.get(metricName);
        Mono<Integer> measuredMono1 = metrics.measure(Mono.just(1));
        measuredMono1.subscribe();
        measuredMono1.subscribe();
        measuredMono1.subscribe();
        Mono<Integer> measuredMono2 = metrics.measure(Mono.just(1));
        measuredMono2.subscribe();
        measuredMono2.subscribe();

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(5);
    }

    @Test
    void shouldMeasurePublisher() {
        String metricName = "test5_publisher";
        Publisher<Integer> publisher = s -> {
            s.onNext(1);
            s.onComplete();
        };
        Publisher<Integer> measuredPublisher = Metrics.get(metricName).measure(publisher);
        measuredPublisher.subscribe(new DoNothingSubscriber<>());

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(1);
    }

    @Test
    void shouldReturnNullWhenPublisherIsNull() {
        String metricName = "test5_publisher";
        Publisher<Integer> measuredPublisher = Metrics.get(metricName).measure((Publisher<Integer>) null);
        assertThat(measuredPublisher).isNull();
    }

    private static class DoNothingSubscriber<T> implements Subscriber<T> {

        @Override
        public void onSubscribe(Subscription subscription) {
        }

        @Override
        public void onNext(T nextElement) {
        }

        @Override
        public void onError(Throwable error) {
        }

        @Override
        public void onComplete() {
        }
    }
}
