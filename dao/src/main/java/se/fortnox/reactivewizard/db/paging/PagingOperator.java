package se.fortnox.reactivewizard.db.paging;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import se.fortnox.reactivewizard.CollectionOptions;

import java.util.function.BiFunction;

public class PagingOperator<T> implements BiFunction<Publisher<T>, CoreSubscriber<T>, CoreSubscriber<T>> {

    private final int limit;
    private final CollectionOptions collectionOptions;

    public PagingOperator(CollectionOptions collectionOptions) {
        this.collectionOptions = collectionOptions;
        this.limit = collectionOptions.getLimit() != null ? collectionOptions.getLimit() : Integer.MAX_VALUE;
    }

    @Override
    public CoreSubscriber<T> apply(Publisher<T> publisher, CoreSubscriber<T> child) {
        return new CoreSubscriber<>() {

            int count = 0;

            @Override
            public void onComplete() {
                collectionOptions.setLastRecord(count <= limit);
                if (!isDisposed(child)) {
                    child.onComplete();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (!isDisposed(child)) {
                    child.onError(throwable);
                }
            }

            @Override
            public void onNext(T item) {
                if (isDisposed(child)) {
                    return;
                }
                count++;
                if (count <= limit) {
                    child.onNext(item);
                }
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                if (!isDisposed(child)) {
                    child.onSubscribe(subscription);
                }
            }

            private static boolean isDisposed(CoreSubscriber<?> coreSubscriber) {
                return coreSubscriber instanceof Disposable disposable && disposable.isDisposed();
            }
        };
    }
}
