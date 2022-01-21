package se.fortnox.reactivewizard.db.transactions;

import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.util.ReactiveDecorator;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static java.util.Arrays.asList;

@Singleton
public class DaoTransactionsFluxImpl implements DaoTransactionsFlux {

    private final TransactionExecutor transactionExecutor;

    @Inject
    public DaoTransactionsFluxImpl() {
        transactionExecutor = new TransactionExecutor();
    }

    @Override
    public <T> Flux<Void> executeTransaction(Iterable<Flux<T>> daoCalls) {
        if (daoCalls == null) {
            return Flux.empty();
        }
        List<StatementContext> statementContexts = transactionExecutor.getStatementContexts(daoCalls, ReactiveDecorator::getDecoration);
        if (statementContexts.isEmpty()) {
            return Flux.empty();
        }
        return Flux.create(subscription -> {
            ConnectionScheduler connectionScheduler = transactionExecutor.getConnectionScheduler(statementContexts);
            connectionScheduler.schedule(subscription::error, connection -> {
                transactionExecutor.executeTransaction(statementContexts, connection);
                subscription.complete();
            });
        });
    }

    @Override
    public <T> Flux<Void> executeTransaction(Flux<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }
}
