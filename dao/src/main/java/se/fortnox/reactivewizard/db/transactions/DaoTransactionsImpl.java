package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import se.fortnox.reactivewizard.util.ReactiveDecorator;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import static java.util.Arrays.asList;
import static rx.Observable.empty;

@Singleton
public class DaoTransactionsImpl implements DaoTransactions {
    private final TransactionExecutor transactionExecutor;

    @Inject
    public DaoTransactionsImpl() {
        transactionExecutor = new TransactionExecutor();
    }

    @Override
    public <T> Observable<Void> executeTransaction(Iterable<Observable<T>> daoCalls) {
        if (daoCalls == null) {
            return empty();
        }

        List<StatementContext> statementContexts = transactionExecutor.getStatementContexts(daoCalls, ReactiveDecorator::getDecoration);
        if (statementContexts.isEmpty()) {
            return empty();
        }
        return Observable.unsafeCreate(subscription -> {
            ConnectionScheduler connectionScheduler = transactionExecutor.getConnectionScheduler(statementContexts);
            connectionScheduler.schedule(subscription::onError, connection -> {
                transactionExecutor.executeTransaction(statementContexts, connection);
                subscription.onCompleted();
            });
        });
    }

    @Override
    public <T> Observable<Void> executeTransaction(Observable<T>... daoCalls) {
        return executeTransaction(asList(daoCalls));
    }
}
