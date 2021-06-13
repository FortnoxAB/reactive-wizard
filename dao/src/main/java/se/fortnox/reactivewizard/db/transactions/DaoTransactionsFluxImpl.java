package se.fortnox.reactivewizard.db.transactions;

import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.util.ReactiveDecorator;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;

public class DaoTransactionsFluxImpl implements DaoTransactionsFlux {
    @Inject
    public DaoTransactionsFluxImpl() {
    }

    <T> void createTransaction(Iterable<Flux<T>> daoCalls) {
        if (daoCalls == null || !daoCalls.iterator().hasNext()) {
            return;
        }

        Transaction transaction = new Transaction(daoCalls);
        for (Flux daoCall : daoCalls) {
            Optional<AtomicReference<TransactionStatement>> statement = ReactiveDecorator.getDecoration(daoCall);
            if (!statement.isPresent()) {
                String statementString  = statement == null ? "null" : statement.getClass().toString();
                String exceptionMessage = "All parameters to createTransaction needs to be Fluxs coming from a Dao-class. Statement was %s.";
                throw new RuntimeException(String.format(exceptionMessage, statementString));
            }
            transaction.add(statement.get());
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
