package se.fortnox.reactivewizard.server;

import javax.inject.Singleton;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
@SuppressWarnings("checkstyle:MissingJavadocMethod")
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

    public void increase() {
        if (connections.getAndIncrement() == 0) {
            connectionsZero.tryAcquire();
        }
    }

    public void decrease() {
        if (connections.decrementAndGet() == 0) {
            connectionsZero.release();
        }
    }

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
