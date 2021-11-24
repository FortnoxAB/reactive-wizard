package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;

public interface DaoTransactions {
    /**
     * Creates a transaction for the passed dao-calls, which means that they
     * will be run as one single transaction, in the order they are passed to
     * this method, when all of the Observables has been subscribed.
     *
     * @param daoCalls dao calls to run as on single transaction
     * @deprecated Use executeTransaction instead.
     */
    @Deprecated
    <T> void createTransaction(Observable<T>... daoCalls);

    /**
     * Creates a transaction for the passed dao-calls, which means that they
     * will be run as one single transaction, in the order they are passed to
     * this method, when all of the Observables has been subscribed.
     *
     * @param daoCalls dao calls to run as on single transaction
     * @deprecated Use executeTransaction instead.
     */
    @Deprecated
    <T> void createTransaction(Iterable<Observable<T>> daoCalls);

    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     */
    <T> Observable<Void> executeTransaction(Iterable<Observable<T>> daoCalls);

    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     */
    <T> Observable<Void> executeTransaction(Observable<T>... daoCalls);
}
