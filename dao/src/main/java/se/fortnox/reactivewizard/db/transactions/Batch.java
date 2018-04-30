package se.fortnox.reactivewizard.db.transactions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;

public class Batch implements Batchable {
    private final LinkedList<TransactionStatement> statements = new LinkedList<>();

    public Batch(TransactionStatement firstStatement, TransactionStatement secondStatement) {
        this.statements.add(firstStatement);
        this.statements.add(secondStatement);
    }

    static Batchable batchWrap(Batchable last, TransactionStatement next) {
        if (last instanceof Batch) {
            ((Batch)last).add(next);
            return last;
        }

        return new Batch((TransactionStatement)last, next);
    }

    private void add(TransactionStatement statement) {
        statements.add(statement);
    }

    @Override
    public boolean sameBatch(Batchable batchable) {
        return statements.peek().sameBatch(batchable);
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = null;
        for (TransactionStatement s : statements) {
            preparedStatement = s.getStatement().batch(connection, preparedStatement);
        }

        int[] ints = preparedStatement.executeBatch();
        for (int i = 0; i < statements.size(); i++) {
            TransactionStatement statement = statements.get(i);
            statement.getStatement().batchExecuted(ints[i]);
        }
    }
}
