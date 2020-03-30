package se.fortnox.reactivewizard.metrics;

import org.junit.Test;
import reactor.core.publisher.Flux;
import rx.Observable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTest {

    @Test
    public void shouldHandleMultipleCallsToMeasure() {
        Metrics             metrics = Metrics.get("test");
        Observable<Integer> m1      = metrics.measure(Observable.just(1));
        Observable<Integer> m2      = metrics.measure(Observable.just(1));
        m1.subscribe();
        m1.subscribe();
        m1.subscribe();
        m2.subscribe();
        m2.subscribe();

        assertThat(metrics.registry().getNames()).contains("test");
        assertThat(metrics.registry().getTimers().get("test").getCount()).isEqualTo(5);

    }

    @Test
    public void shouldMeasureFluxes() {
        String              timerName    = UUID.randomUUID().toString();
        Metrics             metrics = Metrics.get(timerName);
        Observable<Integer> m1      = metrics.measure(Observable.just(1));
        Flux<Integer>       m2      = metrics.measure(Flux.just(1));
        m1.subscribe();
        m1.subscribe();
        m2.subscribe();
        m2.subscribe();
        m2.subscribe();

        assertThat(metrics.registry().getNames()).contains(timerName);
        assertThat(metrics.registry().getTimers().get(timerName).getCount()).isEqualTo(5);

    }
}
