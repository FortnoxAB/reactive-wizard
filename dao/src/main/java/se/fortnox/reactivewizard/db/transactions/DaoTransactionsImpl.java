package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import se.fortnox.reactivewizard.db.statement.Statement;
import se.fortnox.reactivewizard.util.ReactiveDecorator;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static rx.Observable.empty;

@Singleton
public class DaoTransactionsImpl implements DaoTransactions {
    @Inject
    public DaoTransactionsImpl() {
    }

    private  <T> Transaction<T> createTransactionWithStatements(Collection<Observable<T>> daoCalls) {
        List<TransactionStatement> transactionStatements = new ArrayList<>();
        for (StatementContext statementContext : statementContexts) {
            Statement statement = statementContext.getStatement();
            TransactionStatement transactionStatement = new TransactionStatement(statement);
            transactionStatements.add(transactionStatement);
        }

        return new Transaction(transactionStatements);
    }

    @Override
    public <T> Observable<Void> executeTransaction(Iterable<Observable<T>> daoCalls) {
        if (daoCalls == null) {
            return empty();
        }

        List<StatementContext> statementContexts = getStatementContexts(daoCalls, ReactiveDecorator::getDecoration);
        if (statementContexts.isEmpty()) {
            return empty();
        }
        return Observable.unsafeCreate(subscription -> {
            ConnectionScheduler connectionScheduler = getConnectionScheduler(statementContexts);
            connectionScheduler.schedule(subscription::onError, connection -> {
                executeTransaction(statementContexts, connection);
                subscription.onCompleted();
            });
        });
    }

    void executeTransaction(List<StatementContext> statementContexts, Connection connection) throws Exception {
        Transaction transaction = createTransactionWithStatements(statementContexts);
        transaction.execute(connection);
        statementContexts.forEach(StatementContext::transactionCompleted);
    }

    @Override
    public <T> Observable<Void> executeTransaction(Observable<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }

    <T> List<StatementContext> getStatementContexts(Iterable<T> daoCalls, Function<T,Optional<StatementContext>> getDecoration) {
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

    ConnectionScheduler getConnectionScheduler(List<StatementContext> statementContexts) {
        return statementContexts.stream()
            .findFirst()
            .map(StatementContext::getConnectionScheduler)
            .orElseThrow(() -> new RuntimeException("No DaoObservable found"));
    }
}
