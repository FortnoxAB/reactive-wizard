package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

import java.util.function.Supplier;

public class DaoObservable<T> extends Observable<T> {
    private final Observable<T>              result;
    private final Supplier<StatementContext> statementConnectionSchedulerSupplier;
    private final Action0                    onTransactionCompleted;

    public DaoObservable(Observable<T> result, Supplier<StatementContext> statementConnectionSchedulerSupplier) {
        this(result, statementConnectionSchedulerSupplier, null);
    }

    private DaoObservable(Observable<T> result, Supplier<StatementContext> statementConnectionSchedulerSupplier,
        Action0 onTransactionCompleted) {
        super(result::unsafeSubscribe);
        this.result = result;
        this.statementConnectionSchedulerSupplier = statementConnectionSchedulerSupplier;
        this.onTransactionCompleted = onTransactionCompleted;
    }

    public DaoObservable<T> onTerminate(Action0 onTerminate) {
        return new DaoObservable<>(result.doOnTerminate(onTerminate),
            statementConnectionSchedulerSupplier, onTransactionCompleted);
    }

    public DaoObservable<T> onSubscribe(Action0 onSubscribe) {
        return new DaoObservable<>(result.doOnSubscribe(onSubscribe),
            statementConnectionSchedulerSupplier, onTransactionCompleted);
    }

    public DaoObservable<T> onError(Action1<Throwable> onError) {
        return new DaoObservable<>(result.doOnError(onError),
            statementConnectionSchedulerSupplier, onTransactionCompleted);
    }

    public DaoObservable<T> doOnTransactionCompleted(Action0 onCompleted) {
        return new DaoObservable<>(result.doOnCompleted(onCompleted), statementConnectionSchedulerSupplier, onCompleted);
    }

    public void onTransactionCompleted() {
        if (onTransactionCompleted != null) {
            onTransactionCompleted.call();
        }
    }

    public Supplier<StatementContext> getStatementConnectionSchedulerSupplier() {
        return statementConnectionSchedulerSupplier;
    }
}

