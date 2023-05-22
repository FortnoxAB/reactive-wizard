package se.fortnox.reactivewizard.server;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionCounterTest {

    private ConnectionCounter connectionCounter = new ConnectionCounter();

    @Test
    void testZeroConnections() {
        assertTrue(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
    }

    @Test
    void testOneConnection() {
        connectionCounter.increase();
        connectionCounter.decrease();
        assertTrue(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
    }

    @Test
    void testTwoConnections() {
        connectionCounter.increase();
        connectionCounter.increase();
        connectionCounter.decrease();
        connectionCounter.decrease();
        assertTrue(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
    }

    @Test
    void testOneNeverEndingConnection() {
        connectionCounter.increase();
        assertFalse(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
    }

    @Test
    void testOneDelayedConnection() {
        connectionCounter.increase();
        delayed(() -> connectionCounter.decrease());
        assertFalse(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
        assertTrue(connectionCounter.awaitZero(10, TimeUnit.SECONDS));
    }

    @Test
    void shouldReturnFalseWhenExceptionOccuresDuringAwaitZero() throws InterruptedException {
        Semaphore connectionsZero = mock(Semaphore.class);
        when(connectionsZero.tryAcquire(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        ConnectionCounter connectionCounter = new ConnectionCounter(new AtomicLong(), connectionsZero);

        assertFalse(connectionCounter.awaitZero(10, TimeUnit.SECONDS));
    }

    private void delayed(Runnable function) {
        Mono.fromCallable(() -> {
            function.run();
            return true;
        }).delaySubscription(Duration.ofMillis(500)).subscribe();
    }

}
