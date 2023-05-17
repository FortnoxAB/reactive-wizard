package se.fortnox.reactivewizard.server;

import jakarta.inject.Singleton;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class ConnectionCounter {
    private final AtomicLong connections;
    private final Semaphore connectionsZero;

    public ConnectionCounter() {
        this(new AtomicLong(0), new Semaphore(1));
    }

    ConnectionCounter(AtomicLong connections, Semaphore connectionsZero) {
        this.connections = connections;
        this.connectionsZero = connectionsZero;
    }

    /**
     * Increment the number of connections by one.
     */
    public void increase() {
        if (connections.getAndIncrement() == 0) {
            connectionsZero.tryAcquire();
        }
    }

    /**
     * Decrement the number of connections by one.
     */
    public void decrease() {
        if (connections.decrementAndGet() == 0) {
            connectionsZero.release();
        }
    }

    /**
     * Await zero connections.
     * @param time time to wait
     * @param timeUnit unit of time to wait
     * @return whether zero was reached within the time
     */
    public boolean awaitZero(int time, TimeUnit timeUnit) {
        try {
            boolean success = connectionsZero.tryAcquire(time, timeUnit);
            if (success) {
                connectionsZero.release();
            }
            return success;
        } catch (Exception e) {
            return false;
        }
    }

    public long getCount() {
        return connections.get();
    }
}
