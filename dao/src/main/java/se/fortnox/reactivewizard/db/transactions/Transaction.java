package se.fortnox.reactivewizard.db.transactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Transaction {

    private static final Logger LOG = LoggerFactory.getLogger(Transaction.class);

    private final ConcurrentLinkedQueue<TransactionStatement> statementsToExecute;

    Transaction(List<TransactionStatement> statements) {
        this.statementsToExecute = new ConcurrentLinkedQueue<>(statements);
    }

    /**
     * Execute the transation.
     * @param connection the connection
     * @throws Exception on error
     */
    public void execute(Connection connection) throws Exception {
        try {
            executeTransaction(connection);
            closeConnection(connection);
        } catch (Throwable e) {
            rollback(connection);
            closeConnection(connection);
            throw e;
        }
    }

    private void executeTransaction(Connection connection) throws SQLException {
        connection.setAutoCommit(false);

        for (Batchable statement : batchStatementsWherePossible()) {
            statement.execute(connection);
        }

        connection.commit();
    }

    private LinkedList<Batchable> batchStatementsWherePossible() {
        LinkedList<Batchable> statements = new LinkedList<>();

        for (TransactionStatement statement : statementsToExecute) {
            if (!statements.isEmpty() && statements.peekLast().sameBatch(statement)) {
                Batchable last = statements.pollLast(); // Remove last statement
                statements.add(Batch.batchWrap(last, statement)); // Add in batch with current statement
            } else {
                statements.add(statement);
            }
        }
        return statements;
    }

    private void closeConnection(Connection connection) {
        try {
            connection.setAutoCommit(true);
            connection.close();
        } catch (Exception e) {
            LOG.error("Error closing connection", e);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (Exception rollbackException) {
            LOG.error("Rollback failed", rollbackException);
        }
    }
}
