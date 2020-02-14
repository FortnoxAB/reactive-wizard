package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

import java.util.concurrent.atomic.AtomicReference;

public class DaoObservable<T> extends Observable<T> {

    private final AtomicReference<TransactionStatement> transactionStatementRef;
    private final Observable<T>                         result;

    public DaoObservable(Observable<T> result, AtomicReference<TransactionStatement> transactionStatementRef) {
        super(result::unsafeSubscribe);
        this.result = result;
        this.transactionStatementRef = transactionStatementRef;
    }

    void setTransactionStatement(TransactionStatement transactionStatement) {
        transactionStatementRef.set(transactionStatement);
    }

    public DaoObservable<T> onTerminate(Action0 onTerminate) {
        return new DaoObservable<>(result.doOnTerminate(onTerminate), transactionStatementRef);
    }

    public DaoObservable<T> onSubscribe(Action0 onSubscribe) {
        return new DaoObservable<>(result.doOnSubscribe(onSubscribe), transactionStatementRef);
    }

    public DaoObservable<T> onError(Action1<Throwable> onError) {
        return new DaoObservable<>(result.doOnError(onError), transactionStatementRef);
    }

    public DaoObservable<T> doOnTransactionCompleted(Action0 onCompleted) {
        return new DaoObservable<>(result.doOnCompleted(onCompleted), transactionStatementRef);
    }
}

