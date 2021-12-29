package se.fortnox.reactivewizard.db.transactions;

import se.fortnox.reactivewizard.db.statement.Statement;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * class with helper methods used when executing stuff inside a database transaction
 */
class TransactionExecutor {

    /**
     * Creates a Transaction based on a Collection of StatementContexts
     * @param statementContexts the collection of StatementContext to use
     * @return the Transaction
     */
    Transaction createTransactionWithStatements(Collection<StatementContext> statementContexts) {
        List<TransactionStatement> transactionStatements = new ArrayList<>();
        for (StatementContext statementContext : statementContexts) {
            Statement            statement            = statementContext.getStatement();
            TransactionStatement transactionStatement = new TransactionStatement(statement);
            transactionStatements.add(transactionStatement);
        }

        return new Transaction(transactionStatements);
    }

    /**
     * Execute all the statements contained in the list of StatementContext on the provided connection
     * @param statementContexts list of StatementContext to use
     * @param connection the connection to use when executing statements.
     */
    void executeTransaction(List<StatementContext> statementContexts, Connection connection) throws Exception {
        Transaction transaction = createTransactionWithStatements(statementContexts);
        transaction.execute(connection);
        statementContexts.forEach(StatementContext::transactionCompleted);
    }

    /**
     * Extracts a list of StatementContext from the provided dao calls.
     * @param daoCalls the list of dao calls to fetch StatementContext from
     * @param getDecoration a function extracting the decoration from the type in the daoCalls iterable.
     * @param <T> A reactive type Observable, Single, Flux or Mono
     *
     * @return a list of statment contexts.
     */
    <T> List<StatementContext> getStatementContexts(Iterable<T> daoCalls, Function<T, Optional<StatementContext>> getDecoration) {
        List<StatementContext> daoStatementContexts = new ArrayList<>();

        for (T statement : daoCalls) {
            Optional<StatementContext> statementContext = getDecoration.apply(statement);
            if (statementContext.isEmpty()) {
                String statementString  = statement == null ? "null" : statement.getClass().toString();
                String exceptionMessage = "All parameters to createTransaction needs to be observables coming from a Dao-class. Statement was %s.";
                throw new RuntimeException(String.format(exceptionMessage, statementString));
            }
            daoStatementContexts.add(statementContext.get());
        }

        return daoStatementContexts;
    }

    /**
     * Get the connection scheduler to use when executing the statements.
     * Will get the first connection scheduler from the first StatementContext
     *
     * @param statementContexts list of StatementContext
     * @return the ConnectionScheduler to use
     *
     * @throws RuntimeException if a ConnectionScheduler could not be found.
     */
    ConnectionScheduler getConnectionScheduler(List<StatementContext> statementContexts) {
        return statementContexts.stream()
            .findFirst()
            .map(StatementContext::getConnectionScheduler)
            .orElseThrow(() -> new RuntimeException("No DaoObservable found"));
    }

}
