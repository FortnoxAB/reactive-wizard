package se.fortnox.reactivewizard.util.rx;

import org.junit.Test;
import org.mockito.InOrder;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.*;
import static rx.Observable.*;
import static se.fortnox.reactivewizard.util.rx.RxUtils.first;

public class FirstThenTest {

    @Test
    public void shouldExecuteFirstBeforeThen() {
        Consumer<Integer> consumer = mock(Consumer.class);

        Observable<Integer> result = first(just(1).doOnNext(consumer::accept))
            .then(just(2).doOnNext(consumer::accept))
            .thenReturn(just(3).doOnNext(consumer::accept));

        verify(consumer, never()).accept(anyInt());

        assertThat(result.toBlocking().single()).isEqualTo(3);

        InOrder order = inOrder(consumer);
        order.verify(consumer).accept(1);
        order.verify(consumer).accept(2);
        order.verify(consumer).accept(3);
    }

    @Test
    public void shouldExecuteFirstBeforeThenForFunc0Arguments() {
        Consumer<Integer> consumer = mock(Consumer.class);

        first(just(1).doOnNext(consumer::accept))
            .then(() -> { consumer.accept(2); return empty(); })
            .thenReturn(() -> { consumer.accept(3); return empty(); })
            .subscribe();

        InOrder order = inOrder(consumer);
        order.verify(consumer).accept(1);
        order.verify(consumer).accept(2);
        order.verify(consumer).accept(3);
    }

    @Test
    public void shouldReturnNonObservables() {
        String result = first(empty())
            .thenReturn("test")
            .toBlocking()
            .single();
        assertThat(result).isEqualTo("test");
    }

    @Test
    public void shouldReturnEmpty() {
        Boolean result = first(empty())
            .thenReturnEmpty()
            .isEmpty()
            .toBlocking()
            .first();
        assertThat(result).isTrue();
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
