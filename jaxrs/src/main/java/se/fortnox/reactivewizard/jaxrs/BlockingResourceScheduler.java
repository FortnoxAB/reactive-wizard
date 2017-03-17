package se.fortnox.reactivewizard.jaxrs;

import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.inject.Singleton;
import java.util.concurrent.Executors;

@Singleton
public class BlockingResourceScheduler extends Scheduler {

    private final Scheduler scheduler;

    public BlockingResourceScheduler() {
        this(Schedulers.from(Executors.newFixedThreadPool(20)));
    }

    public BlockingResourceScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Worker createWorker() {
        return scheduler.createWorker();
    }
}
