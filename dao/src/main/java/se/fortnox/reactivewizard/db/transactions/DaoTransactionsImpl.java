package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import rx.Scheduler;
import se.fortnox.reactivewizard.db.ConnectionProvider;
import se.fortnox.reactivewizard.db.DbProxy;
import se.fortnox.reactivewizard.db.statement.Statement;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static rx.Observable.empty;

public class DaoTransactionsImpl implements DaoTransactions {
    private final ConnectionProvider connectionProvider;
    private final DbProxy            dbProxy;

    @Inject
    public DaoTransactionsImpl(ConnectionProvider connectionProvider, DbProxy dbProxy) {
        this.connectionProvider = connectionProvider;
        this.dbProxy = dbProxy;
    }

    private  <T> Transaction<T> createTransactionWithStatements(Collection<Observable<T>> daoCalls) {
        List<TransactionStatement> transactionStatements = new ArrayList<>();
        for (Observable<T> daoCall : daoCalls) {
            Statement statement = ((DaoObservable<T>) daoCall).getStatementSupplier().get();
            TransactionStatement transactionStatement = new TransactionStatement(statement);
            transactionStatements.add(transactionStatement);
        }

        return new Transaction<>(connectionProvider, transactionStatements);
    }

    @Override
    public <T> Observable<Void> executeTransaction(Iterable<Observable<T>> daoCalls) {
        if (daoCalls == null || !daoCalls.iterator().hasNext()) {
            return empty();
        }

        Collection<Observable<T>> daoCallsCopy = copyAndVerifyDaoObservables(daoCalls);
        return Observable.unsafeCreate(subscription -> {
            Scheduler.Worker worker = dbProxy.getScheduler().createWorker();
            worker.schedule(() -> {
                try {
                    Transaction<T> transaction = createTransactionWithStatements(daoCallsCopy);
                    transaction.execute();
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

    @Override
    public <T> Observable<Void> executeTransaction(Observable<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }
}
