package se.fortnox.reactivewizard.db.transactions;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.util.ReactiveDecorator;

import java.util.List;

import static java.util.Arrays.asList;
import static reactor.core.publisher.Mono.empty;

@Singleton
public class DaoTransactionsImpl implements DaoTransactions {
    private final TransactionExecutor transactionExecutor;

    @Inject
    public DaoTransactionsImpl() {
        transactionExecutor = new TransactionExecutor();
    }

    @Override
    public Mono<Void> executeTransaction(Iterable<? extends Publisher<?>> daoCalls) {
        if (daoCalls == null) {
            return empty();
        }
        List<StatementContext> statementContexts = transactionExecutor.getStatementContexts(daoCalls, ReactiveDecorator::getDecoration);
        if (statementContexts.isEmpty()) {
            return empty();
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
    public Mono<Void> executeTransaction(Publisher<?>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }
}
