package se.fortnox.reactivewizard.db.transactions;

import reactor.core.publisher.Flux;

public interface DaoTransactionsFlux {
    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     */
    <T> Flux<Void> executeTransaction(Iterable<Flux<T>> daoCalls);

    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     */
    <T> Flux<Void> executeTransaction(Flux<T>... daoCalls);
}
