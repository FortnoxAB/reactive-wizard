package se.fortnox.reactivewizard.db.transactions;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface DaoTransactions {
    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     * @return empty Flux
     */
    <T> Mono<Void> executeTransaction(Iterable<? extends Publisher<T>> daoCalls);

    /**
     * Creates and executes a transaction for the passed dao-calls.
     * The calls will be run in the order they are passed to this method.
     *
     * @param daoCalls dao calls to run as on single transaction
     * @return  empty Flux
     */
    <T> Mono<Void> executeTransaction(Publisher<T>... daoCalls);
}
