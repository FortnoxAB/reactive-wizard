package se.fortnox.reactivewizard.db.transactions;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import se.fortnox.reactivewizard.db.statement.Statement;

import java.util.function.Supplier;

public class DaoObservable<T> extends Observable<T> {
    private final Observable<T> result;
    private final Supplier<Statement> statementSupplier;

    public DaoObservable(Observable<T> result, Supplier<Statement> statementSupplier) {
        super(result::unsafeSubscribe);
        this.result = result;
        this.statementSupplier = statementSupplier;
    }

    public DaoObservable<T> onTerminate(Action0 onTerminate) {
        return new DaoObservable<>(result.doOnTerminate(onTerminate), statementSupplier);
    }

    public DaoObservable<T> onSubscribe(Action0 onSubscribe) {
        return new DaoObservable<>(result.doOnSubscribe(onSubscribe), statementSupplier);
    }

    public DaoObservable<T> onError(Action1<Throwable> onError) {
        return new DaoObservable<>(result.doOnError(onError), statementSupplier);
    }

    public Supplier<Statement> getStatementSupplier() {
        return statementSupplier;
    }
}

