package se.fortnox.reactivewizard.db.transactions;

import rx.Scheduler;
import rx.Subscriber;
import se.fortnox.reactivewizard.db.ConnectionProvider;

import java.sql.Connection;

public class ConnectionScheduler {
    private final ConnectionProvider connectionProvider;
    private final Scheduler scheduler;

    public ConnectionScheduler(ConnectionProvider connectionProvider, Scheduler scheduler) {
        this.connectionProvider = connectionProvider;
        this.scheduler = scheduler;
    }

    public void schedule(Subscriber<?> subscription, ThrowableAction action) {
        Scheduler.Worker worker = scheduler.createWorker();
        worker.schedule(() -> {
            try {
                action.call(connectionProvider.get());
            } catch (Exception e) {
                if (!subscription.isUnsubscribed()) {
                    subscription.onError(e);
                }
            } finally {
                worker.unsubscribe();
            }
        });
    }

    public interface ThrowableAction {
        void call(Connection connection) throws Exception;
    }
}
