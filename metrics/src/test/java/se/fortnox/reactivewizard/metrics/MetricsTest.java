package se.fortnox.reactivewizard.metrics;

import org.junit.Test;
import rx.Observable;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsTest {

    @Test
    public void shouldMeasureObservable() {
        String metricName = "test1";
        Observable<Integer> measuredObservable = Metrics.get(metricName)
            .measure(Observable.just(1));
        measuredObservable.subscribe();

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(1);
    }

    @Test
    public void shouldStopMeasuringOnError() {
        String metricName = "test2";
        Observable<Integer> measuredObservable = Metrics.get(metricName)
            .measure(Observable.error(new Exception()));
        measuredObservable.test()
            .assertError(Exception.class)
            .awaitTerminalEvent();

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(1);
    }

    @Test
    public void shouldHandleNullObservables() {
        String metricName = "test3";
        Observable<Integer> measuredObservable = Metrics.get(metricName)
            .measure(null);

        assertThat(measuredObservable).isNull();
    }

    @Test
    public void shouldHandleMultipleCallsToMeasure() {
        String              metricName          = "test4";
        Metrics             metrics             = Metrics.get(metricName);
        Observable<Integer> measuredObservable1 = metrics.measure(Observable.just(1));
        Observable<Integer> measuredObservable2 = metrics.measure(Observable.just(1));
        measuredObservable1.subscribe();
        measuredObservable1.subscribe();
        measuredObservable1.subscribe();
        measuredObservable2.subscribe();
        measuredObservable2.subscribe();

        assertThat(Metrics.registry().getNames()).contains(metricName);
        assertThat(Metrics.registry().getTimers().get(metricName).getCount()).isEqualTo(5);
    }
}
