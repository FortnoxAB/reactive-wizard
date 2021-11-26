package se.fortnox.reactivewizard.db.transactions;

import se.fortnox.reactivewizard.db.statement.Statement;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionStatement implements Batchable {
    private final Statement statement;

    public TransactionStatement(Statement statement) {
        this.statement = statement;
    }

    public Statement getStatement() {
        return statement;
    }

    @Override
    public boolean sameBatch(Batchable batchable) {
        return batchable instanceof TransactionStatement
            && getStatement().sameBatch(((TransactionStatement)batchable).getStatement());
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        getStatement().execute(connection);
    }
}
