package se.fortnox.reactivewizard.metrics;

import org.junit.Test;
import rx.Observable;

import static org.fest.assertions.Assertions.assertThat;

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
}
