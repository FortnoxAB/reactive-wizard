package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import rx.functions.Action0;

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

    public DaoObservable<T> doOnTransactionCompleted(Action0 onCompleted) {
        return new DaoObservable<>(result.doOnCompleted(onCompleted), transactionStatementRef);
    }
}

