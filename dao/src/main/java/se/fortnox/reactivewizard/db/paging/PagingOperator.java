package se.fortnox.reactivewizard.db.paging;

import rx.Observable.Operator;
import rx.Subscriber;
import se.fortnox.reactivewizard.CollectionOptions;

public class PagingOperator<T> implements Operator<T, T> {

    private final int                         limit;
    private       CollectionOptionsWithResult collectionOptions;

    public PagingOperator(CollectionOptions collectionOptions) {
        if (collectionOptions instanceof CollectionOptionsWithResult) {
            this.collectionOptions = (CollectionOptionsWithResult)collectionOptions;
        }
        this.limit = collectionOptions.getLimit();
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {

            int count = 0;

            @Override
            public void onCompleted() {
                if (!child.isUnsubscribed()) {
                    child.onCompleted();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (!child.isUnsubscribed()) {
                    child.onError(throwable);
                }
            }

            @Override
            public void onNext(T item) {
                if (child.isUnsubscribed()) {
                    return;
                }
                count++;
                if (count <= limit) {
                    child.onNext(item);
                } else if (collectionOptions != null) {
                    collectionOptions.setNotLastRecord();
                }
            }
        };
    }
}
