package se.fortnox.reactivewizard.db.transactions;

import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.util.ReactiveDecorator;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static java.util.Arrays.asList;

@Singleton
public class DaoTransactionsFluxImpl implements DaoTransactionsFlux {
    private final DaoTransactionsImpl daoTransactions;

    @Inject
    public DaoTransactionsFluxImpl(DaoTransactionsImpl daoTransactions) {
        this.daoTransactions = daoTransactions;
    }

    @Override
    public <T> Flux<Void> executeTransaction(Iterable<Flux<T>> daoCalls) {
        if (daoCalls == null) {
            return Flux.empty();
        }
        List<StatementContext> statementContexts = daoTransactions.getStatementContexts(daoCalls, ReactiveDecorator::getDecoration);
        if (statementContexts.isEmpty()) {
            return Flux.empty();
        }
        return Flux.create(subscription -> {
            ConnectionScheduler connectionScheduler = daoTransactions.getConnectionScheduler(statementContexts);
            connectionScheduler.schedule(subscription::error, connection -> {
                daoTransactions.executeTransaction(statementContexts, connection);
                subscription.complete();
            });
        });
    }

    @Override
    public <T> Flux<Void> executeTransaction(Flux<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }
}
