package se.fortnox.reactivewizard.util;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import java.util.concurrent.atomic.AtomicInteger;

import static reactor.core.publisher.Flux.from;
import static reactor.test.StepVerifier.create;
import static se.fortnox.reactivewizard.util.rx.RetryWithDelay.retryWithDelay;

public class RetryWithDelayTest {

    private Publisher<Integer> getRetryPublisher() {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        return Flux.create(fluxSink -> {
            if (atomicInteger.getAndIncrement() == 0) {
                fluxSink.error(new RuntimeException());
                return;
            }

            fluxSink.next(1);
            fluxSink.complete();
        });
    }

    @Test
    public void shouldRetryWithPredicate() {
        Publisher<Integer> retryPublisher = from(getRetryPublisher())
            .retryWhen(retryWithDelay(1, 1, throwable -> true));

        create(retryPublisher).expectNextCount(1).verifyComplete();
    }

    @Test
    public void shouldNotRetryWithPredicate() {
        Publisher<Integer> retryPublisher = from(getRetryPublisher())
            .retryWhen(retryWithDelay(1, 1, throwable -> false));

        create(retryPublisher).verifyError();
    }

    @Test
    public void shouldRetryForCertainError() {
        Publisher<Integer> retryPublisher = from(getRetryPublisher())
            .retryWhen(retryWithDelay(1, 1, RuntimeException.class));

        create(retryPublisher).expectNextCount(1).verifyComplete();
    }

    @Test
    public void shouldNotRetryForCertainError() {
        Publisher<Integer> retryPublisher = from(getRetryPublisher())
            .retryWhen(retryWithDelay(1, 1, NumberFormatException.class));

        create(retryPublisher).verifyError();
    }

}
