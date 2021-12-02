package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import rx.Scheduler;
import se.fortnox.reactivewizard.db.ConnectionProvider;
import se.fortnox.reactivewizard.db.statement.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static rx.Observable.empty;

public class DaoTransactionsImpl implements DaoTransactions {

    private  <T> Transaction<T> createTransactionWithStatements(Collection<Observable<T>> daoCalls) {
        List<TransactionStatement> transactionStatements = new ArrayList<>();
        ConnectionProvider connectionProvider = null;
        Scheduler scheduler = null;
        for (Observable<T> daoCall : daoCalls) {
            StatementConnectionScheduler transactionHolder = ((DaoObservable<T>) daoCall).getStatementConnectionSchedulerSupplier().get();
            Statement statement = transactionHolder.statement();
            TransactionStatement transactionStatement = new TransactionStatement(statement);
            transactionStatements.add(transactionStatement);

            if (connectionProvider == null) {
                connectionProvider = transactionHolder.connectionProvider();
            }

            if (scheduler == null) {
                scheduler = transactionHolder.scheduler();
            }
        }

        return new Transaction<>(connectionProvider, scheduler, transactionStatements);
    }

    @Override
    public <T> Observable<Void> executeTransaction(Iterable<Observable<T>> daoCalls) {
        if (daoCalls == null || !daoCalls.iterator().hasNext()) {
            return empty();
        }

        Collection<Observable<T>> daoCallsCopy = copyAndVerifyDaoObservables(daoCalls);
        return Observable.unsafeCreate(subscription -> {
            Transaction<T> transaction = createTransactionWithStatements(daoCallsCopy);
            Scheduler.Worker worker = transaction.getScheduler().createWorker();
            worker.schedule(() -> {
                try {
                    transaction.execute();
                    daoCalls.forEach(daoCall -> ((DaoObservable<T>) daoCall).onTransactionCompleted());
                    subscription.onCompleted();
                } catch (Exception e) {
                    if (!subscription.isUnsubscribed()) {
                        subscription.onError(e);
                    }
                } finally {
                    worker.unsubscribe();
                }
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
}
