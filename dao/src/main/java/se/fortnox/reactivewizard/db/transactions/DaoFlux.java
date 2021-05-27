package se.fortnox.reactivewizard.db.transactions;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import rx.RxReactiveStreams;

public class DaoFlux<T> extends Flux<T> {
    private final DaoObservable daoObservable;

    public DaoFlux(DaoObservable daoObservable) {
        this.daoObservable = daoObservable;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> coreSubscriber) {
        RxReactiveStreams.toPublisher(daoObservable).subscribe(coreSubscriber);
    }

    DaoObservable toDaoObservable() {
        return daoObservable;
    }
}
