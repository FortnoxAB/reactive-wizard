package se.fortnox.reactivewizard.db.transactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.db.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Transaction<T> {

    private static final Logger                                      log                 = LoggerFactory.getLogger(Transaction.class);
    private final        ConnectionProvider                          connectionProvider;
    private final        ConcurrentLinkedQueue<TransactionStatement> statementsToExecute;
    private final        AtomicBoolean                               waitingForExecution = new AtomicBoolean(true);

    Transaction(ConnectionProvider connectionProvider, List<TransactionStatement> statements) {
        this.connectionProvider = connectionProvider;
        this.statementsToExecute = new ConcurrentLinkedQueue<>(statements);
    }

    public void execute() throws Exception {
        if (!waitingForExecution.compareAndSet(true, false)) {
            return;
        }

        Connection connection = connectionProvider.get();
        try {
            executeTransaction(connection);
            closeConnection(connection);
        } catch (Exception e) {
            rollback(connection);
            closeConnection(connection);
            waitingForExecution.compareAndSet(false, true);
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
            log.error("Error closing connection", e);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (Exception rollbackException) {
            log.error("Rollback failed", rollbackException);
        }
    }
}
