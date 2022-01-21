package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;

public interface DaoTransactions {

    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     * @return empty Observable
     */
    <T> Observable<Void> executeTransaction(Iterable<Observable<T>> daoCalls);

    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     * @return empty Observable
     */
    <T> Observable<Void> executeTransaction(Observable<T>... daoCalls);
}
