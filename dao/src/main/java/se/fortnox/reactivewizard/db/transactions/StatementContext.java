package se.fortnox.reactivewizard.db.transactions;

import se.fortnox.reactivewizard.db.statement.Statement;

import java.util.function.Supplier;

public final class StatementContext {
    private final Supplier<Statement> statementSupplier;
    private final ConnectionScheduler connectionScheduler;
    private Runnable transactionCompletedAction;

    public StatementContext(Supplier<Statement> statementSupplier, ConnectionScheduler connectionScheduler) {
        this.statementSupplier = statementSupplier;
        this.connectionScheduler = connectionScheduler;
    }

    public Statement getStatement() {
        return statementSupplier.get();
    }

    public ConnectionScheduler getConnectionScheduler() {
        return connectionScheduler;
    }

    public void onTransactionCompleted(Runnable action) {
        this.transactionCompletedAction = action;
    }

    void transactionCompleted() {
        if (this.transactionCompletedAction != null) {
            this.transactionCompletedAction.run();
        }
    }
}
