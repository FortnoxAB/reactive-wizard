package se.fortnox.reactivewizard.util.rx;

import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.function.Consumer;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static rx.Observable.empty;
import static rx.Observable.error;
import static rx.Observable.just;
import static rx.Observable.range;
import static se.fortnox.reactivewizard.util.rx.RxUtils.first;

public class FirstThenTest {

    @Test
    public void shouldExecuteFirstBeforeThen() {
        Consumer<Integer>   log    = mock(Consumer.class);
        Observable<Integer> result = first(just(1).doOnNext(log::accept)).thenReturn(just(2));
        verify(log, never()).accept(1);

        assertThat(result.toBlocking().single()).isEqualTo(2);

        verify(log).accept(1);
    }

    @Test
    public void shouldNotExecuteSecondIfFirstFails() {
        Consumer<Integer> log = mock(Consumer.class);

        try {
            first(error(new Exception("err"))).thenReturn(just(2).doOnNext(log::accept)).toBlocking().single();
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).isEqualTo("err");
        }

        verify(log, never()).accept(2);
    }

    @Test
    public void shouldRunObservableInFunction() {
        Consumer<Integer> log = mock(Consumer.class);
        Integer result = first(just(1))
            .then(() -> Observable.fromCallable(() -> {
                log.accept(2);
                return "test";
            }))
            .thenReturn(just(3))
            .toBlocking()
            .single();

        assertThat(result).isEqualTo(3);
        verify(log).accept(2);
    }

    @Test
    public void shouldSupportBackpressure() {
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>(0);
        first(empty()).thenReturn(range(1, 100)).subscribe(testSubscriber);

        testSubscriber.assertNoValues();

        testSubscriber.requestMore(1);

        testSubscriber.assertValueCount(1);
    }
}
