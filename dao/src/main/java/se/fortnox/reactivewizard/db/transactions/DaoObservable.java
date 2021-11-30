package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import se.fortnox.reactivewizard.db.statement.Statement;

import java.util.function.Supplier;

public class DaoObservable<T> extends Observable<T> {
    private final Observable<T> result;
    private final Supplier<Statement> statementSupplier;
    private final Action0 onTransactionCompleted;

    public DaoObservable(Observable<T> result, Supplier<Statement> statementSupplier) {
        this(result, statementSupplier, null);
    }

    private DaoObservable(Observable<T> result, Supplier<Statement> statementSupplier, Action0 onTransactionCompleted) {
        super(result::unsafeSubscribe);
        this.result = result;
        this.statementSupplier = statementSupplier;
        this.onTransactionCompleted = onTransactionCompleted;
    }

    public DaoObservable<T> onTerminate(Action0 onTerminate) {
        return new DaoObservable<>(result.doOnTerminate(onTerminate), statementSupplier, onTransactionCompleted);
    }

    public DaoObservable<T> onSubscribe(Action0 onSubscribe) {
        return new DaoObservable<>(result.doOnSubscribe(onSubscribe), statementSupplier, onTransactionCompleted);
    }

    public DaoObservable<T> onError(Action1<Throwable> onError) {
        return new DaoObservable<>(result.doOnError(onError), statementSupplier, onTransactionCompleted);
    }

    public DaoObservable<T> doOnTransactionCompleted(Action0 onCompleted) {
        return new DaoObservable<>(result.doOnCompleted(onCompleted), statementSupplier, onCompleted);
    }

    public void onTransactionCompleted() {
        if (onTransactionCompleted != null) {
            onTransactionCompleted.call();
        }
    }

    public Supplier<Statement> getStatementSupplier() {
        return statementSupplier;
    }
}

