package se.fortnox.reactivewizard.db.transactions;

import rx.Scheduler;
import rx.Subscriber;
import se.fortnox.reactivewizard.db.ConnectionProvider;

import java.sql.Connection;
import java.util.function.Consumer;

public class ConnectionScheduler {
    private final ConnectionProvider connectionProvider;
    private final Scheduler scheduler;

    public ConnectionScheduler(ConnectionProvider connectionProvider, Scheduler scheduler) {
        this.connectionProvider = connectionProvider;
        this.scheduler = scheduler;
    }

    boolean hasConnectionProvider() {
        return connectionProvider != null;
    }

    public void schedule(Consumer<Throwable> onError, ThrowableAction action) {
        Scheduler.Worker worker = scheduler.createWorker();
        worker.schedule(() -> {
            try {
                action.call(connectionProvider.get());
            } catch (Exception e) {
                onError.accept(e);
            } finally {
                worker.unsubscribe();
            }
        });
    }

    public interface ThrowableAction {
        void call(Connection connection) throws Exception;
    }
}
