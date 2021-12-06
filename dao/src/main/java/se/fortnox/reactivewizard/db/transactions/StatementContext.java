package se.fortnox.reactivewizard.db.transactions;

import se.fortnox.reactivewizard.db.statement.Statement;

public final class StatementContext {
    private final Statement statement;
    private final ConnectionScheduler connectionScheduler;

    public StatementContext(Statement statement, ConnectionScheduler connectionScheduler) {
        this.statement = statement;
        this.connectionScheduler = connectionScheduler;
    }

    public Statement getStatement() {
        return statement;
    }

    public ConnectionScheduler getConnectionScheduler() {
        return connectionScheduler;
    }
}
