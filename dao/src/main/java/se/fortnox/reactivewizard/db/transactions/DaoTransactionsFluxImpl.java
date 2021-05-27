package se.fortnox.reactivewizard.db.transactions;

import reactor.core.publisher.Flux;

import javax.inject.Inject;

import static java.util.Arrays.asList;

public class DaoTransactionsFluxImpl implements DaoTransactionsFlux {
    @Inject
    public DaoTransactionsFluxImpl() {
    }

    <T> void createTransaction(Iterable<Flux<T>> daoCalls) {
        if (daoCalls == null || !daoCalls.iterator().hasNext()) {
            return;
        }

        for (Flux statement : daoCalls) {
            if (!(statement instanceof DaoFlux)) {
                String statementString  = statement == null ? "null" : statement.getClass().toString();
                String exceptionMessage = "All parameters to createTransaction needs to be Fluxs coming from a Dao-class. Statement was %s.";
                throw new RuntimeException(String.format(exceptionMessage, statementString));
            }
        }

        Transaction transaction = new Transaction(daoCalls);
        for (Flux statement : daoCalls) {
            transaction.add(((DaoFlux)statement).toDaoObservable());
        }
    }

    @Override
    public <T> Flux<Void> executeTransaction(Iterable<Flux<T>> daoCalls) {
        if (!daoCalls.iterator().hasNext()) {
            return Flux.empty();
        }
        createTransaction(daoCalls);
        return Flux.merge(daoCalls).ignoreElements().cast(Void.class).flux();
    }

    @Override
    public <T> Flux<Void> executeTransaction(Flux<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }
}
