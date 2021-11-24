package se.fortnox.reactivewizard.db.transactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import se.fortnox.reactivewizard.db.ConnectionProvider;
import se.fortnox.reactivewizard.db.DbProxy;

import javax.inject.Inject;

import static java.util.Arrays.asList;
import static rx.Observable.empty;

public class DaoTransactionsImpl implements DaoTransactions {
    private static final Logger LOG = LoggerFactory.getLogger(DaoTransactionsImpl.class);

    private final ConnectionProvider connectionProvider;
    private final DbProxy            dbProxy;

    @Inject
    public DaoTransactionsImpl(ConnectionProvider connectionProvider, DbProxy dbProxy) {
        this.connectionProvider = connectionProvider;
        this.dbProxy = dbProxy;
    }

    @Override
    public <T> void createTransaction(Observable<T>... daoCalls) {
        LOG.info("createTransaction used. Remove and rewrite transaction when sure those are no longer used.");
        if (daoCalls == null || daoCalls.length == 0) {
            return;
        }

        createTransaction(asList(daoCalls));
    }

    @Override
    public <T> void createTransaction(Iterable<Observable<T>> daoCalls) {
        LOG.info("createTransaction used. Remove and rewrite transaction when sure those are no longer used.");
        if (daoCalls == null || !daoCalls.iterator().hasNext()) {
            return;
        }

        createTransactionWithStatements(daoCalls);
    }

    public <T> Transaction<T> createTransactionWithStatements(Iterable<Observable<T>> daoCalls) {
        for (Observable statement : daoCalls) {
            if (!(statement instanceof DaoObservable)) {
                String statementString  = statement == null ? "null" : statement.getClass().toString();
                String exceptionMessage = "All parameters to createTransaction needs to be observables coming from a Dao-class. Statement was %s.";
                throw new RuntimeException(String.format(exceptionMessage, statementString));
            }
        }

        Transaction<T> transaction = new Transaction<>(connectionProvider, daoCalls);
        for (Observable<T> statement : daoCalls) {
            transaction.add((DaoObservable<T>) statement);
        }

        return transaction;
    }

    @Override
    public <T> Observable<Void> executeTransaction(Iterable<Observable<T>> daoCalls) {
        if (!daoCalls.iterator().hasNext()) {
            return empty();
        }
        Transaction<T> transaction = createTransactionWithStatements(daoCalls);

        for (Observable<T> statement : daoCalls) {
            DaoObservable<T> daoCall = (DaoObservable<T>) statement;
            TransactionStatement transactionStatement = daoCall.getTransactionStatement();
            transactionStatement.markStatementSubscribed(daoCall.getStatement());
            transaction.markSubscribed(transactionStatement);
        }


        return Observable.unsafeCreate(subscription -> {
            transaction.onTransactionFailed(throwable -> {
                if (!subscription.isUnsubscribed()) {
                    subscription.onError(throwable);
                }
            });

            Scheduler.Worker worker = dbProxy.getScheduler().createWorker();
            worker.schedule(() -> {
                try {
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

    @Override
    public <T> Observable<Void> executeTransaction(Observable<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }
}
