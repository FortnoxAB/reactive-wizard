package se.fortnox.reactivewizard.db.transactions;

import reactor.core.publisher.Mono;

public interface DaoTransactionsFlux {
    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     * @return empty Flux
     */
    <T> Mono<Void> executeTransaction(Iterable<Mono<T>> daoCalls);

    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     * @return  empty Flux
     */
    <T> Mono<Void> executeTransaction(Mono<T>... daoCalls);
}
