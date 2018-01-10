package se.fortnox.reactivewizard.server;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class RxSyncThreadFactory implements ThreadFactory {
    private final String prefix = "RxSync";
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new RxSyncThread(r, prefix + counter.incrementAndGet());
        t.setDaemon(true);
        return t;
    }
}
