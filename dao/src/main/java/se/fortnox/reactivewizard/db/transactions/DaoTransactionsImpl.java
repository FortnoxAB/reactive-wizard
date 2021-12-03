package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import se.fortnox.reactivewizard.db.statement.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static rx.Observable.empty;

public class DaoTransactionsImpl implements DaoTransactions {

    private  <T> Transaction<T> createTransactionWithStatements(Collection<Observable<T>> daoCalls) {
        List<TransactionStatement> transactionStatements = new ArrayList<>();
        for (Observable<T> daoCall : daoCalls) {
            StatementContext transactionHolder = ((DaoObservable<T>) daoCall).getStatementContextSupplier().get();
            Statement statement = transactionHolder.getStatement();
            TransactionStatement transactionStatement = new TransactionStatement(statement);
            transactionStatements.add(transactionStatement);
        }

        return new Transaction<>(transactionStatements);
    }

    @Override
    public <T> Observable<Void> executeTransaction(Iterable<Observable<T>> daoCalls) {
        if (daoCalls == null || !daoCalls.iterator().hasNext()) {
            return empty();
        }

        Collection<Observable<T>> daoCallsCopy = copyAndVerifyDaoObservables(daoCalls);
        return Observable.unsafeCreate(subscription -> {
            ConnectionScheduler connectionScheduler = getConnectionScheduler(daoCallsCopy);
            connectionScheduler.schedule(subscription, connection -> {
                Transaction<T> transaction = createTransactionWithStatements(daoCallsCopy);
                transaction.execute(connection);
                daoCalls.forEach(daoCall -> ((DaoObservable<T>) daoCall).onTransactionCompleted());
                subscription.onCompleted();
            });
        });
    }

    @Override
    public <T> Observable<Void> executeTransaction(Observable<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }

    private <T> Collection<Observable<T>> copyAndVerifyDaoObservables(Iterable<Observable<T>> daoCalls) {
        List<Observable<T>> daoCallsCopy = new ArrayList<>();
        for (Observable<T> statement : daoCalls) {
            if (!(statement instanceof DaoObservable)) {
                String statementString  = statement == null ? "null" : statement.getClass().toString();
                String exceptionMessage = "All parameters to createTransaction needs to be observables coming from a Dao-class. Statement was %s.";
                throw new RuntimeException(String.format(exceptionMessage, statementString));
            }

            daoCallsCopy.add(statement);
        }

        return daoCallsCopy;
    }

    private <T> ConnectionScheduler getConnectionScheduler(Collection<Observable<T>> daoCallsCopy) {
        Observable<T> daoObservable = daoCallsCopy.stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No DaoObservable found"));

        return ((DaoObservable<T>) daoObservable).getStatementContextSupplier().get().getConnectionScheduler();
    }
}
