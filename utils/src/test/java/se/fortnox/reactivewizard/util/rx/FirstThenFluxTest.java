package se.fortnox.reactivewizard.util.rx;

import org.junit.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static reactor.core.publisher.Flux.empty;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Flux.range;
import static se.fortnox.reactivewizard.util.rx.FirstThenFlux.first;

public class FirstThenFluxTest {

    @Test
    public void shouldExecuteFirstBeforeThen() {
        Consumer<Integer> consumer = mock(Consumer.class);

        Flux<Integer> result = first(just(1).doOnNext(consumer))
            .then(just(2).doOnNext(consumer))
            .thenReturn(just(3).doOnNext(consumer));

        verify(consumer, never()).accept(anyInt());

        assertThat(result.blockFirst()).isEqualTo(3);

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
            .blockFirst();
        assertThat(result).isEqualTo("test");
    }

    @Test
    public void shouldReturnEmpty() {
        Long numberOfElements = first(empty())
            .thenReturnEmpty()
            .count().block();
        assertThat(numberOfElements).isEqualTo(0);
    }

    @Test
    public void shouldNotExecuteSecondIfFirstFails() {
        Consumer<Integer> log = mock(Consumer.class);

        try {
            first(error(new Exception("err"))).thenReturn(just(2).doOnNext(log)).blockLast();
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
            .then(() -> Flux.defer(() -> {
                log.accept(2);
                return Flux.just("test");
            }))
            .thenReturn(just(3))
            .blockLast();

        assertThat(result).isEqualTo(3);
        verify(log).accept(2);
    }

    @Test
    public void shouldSupportBackpressure() {
        final Flux<Integer> integerFlux = first(empty()).thenReturn(range(1, 100));

        StepVerifier.create(integerFlux)
            .expectSubscription()
            .thenRequest(2)
            .expectNext(1, 2)
            .thenRequest(98)
            .expectNextCount(98)
            .verifyComplete();
    }
}
