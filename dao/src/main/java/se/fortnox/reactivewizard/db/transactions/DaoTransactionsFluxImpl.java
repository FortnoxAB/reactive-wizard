package se.fortnox.reactivewizard.db.transactions;

import reactor.core.publisher.Mono;
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
    public <T> Mono<Void> executeTransaction(Iterable<Mono<T>> daoCalls) {
        if (daoCalls == null) {
            return Mono.empty();
        }
        List<StatementContext> statementContexts = transactionExecutor.getStatementContexts(daoCalls, ReactiveDecorator::getDecoration);
        if (statementContexts.isEmpty()) {
            return Mono.empty();
        }
        return Mono.create(subscription -> {
            ConnectionScheduler connectionScheduler = transactionExecutor.getConnectionScheduler(statementContexts);
            connectionScheduler.schedule(subscription::error, connection -> {
                transactionExecutor.executeTransaction(statementContexts, connection);
                subscription.success();
            });
        });
    }

    @Override
    public <T> Mono<Void> executeTransaction(Mono<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }
}
