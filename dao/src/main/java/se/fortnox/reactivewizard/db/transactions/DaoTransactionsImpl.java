package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import se.fortnox.reactivewizard.util.ReactiveDecorator;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static rx.Observable.empty;
import static rx.Observable.merge;

public class DaoTransactionsImpl implements DaoTransactions {
    @Inject
    public DaoTransactionsImpl() {
    }

    @Override
    public <T> void createTransaction(Observable<T>... daoCalls) {
        if (daoCalls == null || daoCalls.length == 0) {
            return;
        }

        createTransaction(asList(daoCalls));
    }

    @Override
    public <T> void createTransaction(Iterable<Observable<T>> daoCalls) {
        if (daoCalls == null || !daoCalls.iterator().hasNext()) {
            return;
        }

        Transaction transaction = new Transaction(daoCalls);
        for (Observable daoCall : daoCalls) {
            Optional<AtomicReference<TransactionStatement>> statement = ReactiveDecorator.getDecoration(daoCall);
            if (!statement.isPresent()) {
                String statementString  = statement == null ? "null" : statement.getClass().toString();
                String exceptionMessage = "All parameters to createTransaction needs to be observables coming from a Dao-class. Statement was %s.";
                throw new RuntimeException(String.format(exceptionMessage, statementString));
            }
            transaction.add(statement.get());
        }
    }

    @Override
    public <T> Observable<Void> executeTransaction(Iterable<Observable<T>> daoCalls) {
        if (!daoCalls.iterator().hasNext()) {
            return empty();
        }
        createTransaction(daoCalls);
        return merge(daoCalls).ignoreElements().cast(Void.class);
    }

    @Override
    public <T> Observable<Void> executeTransaction(Observable<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }
}
