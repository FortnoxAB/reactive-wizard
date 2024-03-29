package se.fortnox.reactivewizard.db.transactions;

import reactor.core.scheduler.Scheduler;
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

    /**
     * Schedule action.
     *
     * @param onError the error handler
     * @param action  the action
     */
    public void schedule(Consumer<Throwable> onError, ThrowableAction action) {
        Scheduler.Worker worker = scheduler.createWorker();
        worker.schedule(() -> {
            try {
                action.call(connectionProvider.get());
            } catch (Exception e) {
                onError.accept(e);
            } finally {
                worker.dispose();
            }
        });
    }

    public interface ThrowableAction {
        void call(Connection connection) throws Exception;
    }
}
