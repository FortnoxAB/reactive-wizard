package se.fortnox.reactivewizard.util.rx;

import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action2;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static rx.Observable.defer;
import static rx.Observable.error;

/**
 * Utility class for various rxjava operations.
 */
public class RxUtils {

    public static FirstThen first(Observable<?> doFirst) {
        return FirstThen.first(doFirst);
    }

    public static <T> IfThenElse<T> ifTrue(Observable<Boolean> ifValue) {
        return new IfThenElse<T>(ifValue);
    }

    /**
     * Consolidate two streams into one using a callback.
     *
     * @param observableLeft Input stream.
     * @param observableRight Second stream that merges into the first.
     * @param action Action to call on each pair in the streams.
     * @return The consolidated stream.
     */
    public static <T1, T2> Observable<T1> consolidate(Observable<T1> observableLeft, Observable<T2> observableRight, Action2<T1, T2> action) {
        return Observable.zip(observableLeft, observableRight, (leftObj, rightObj) -> {
            action.call(leftObj, rightObj);
            return leftObj;
        });
    }

    /**
     * Converts a list into an asynchronous observable stream.<br>
     * This is useful when multiple longer operations are involved that can run simultaneously.<br>
     * <br>
     * <b>Example usage:</b><br>
     * <pre>
     * <code>orderResource.create(order)
     *       .flatMap(order -&gt; RxUtils.async(order.getRows()))
     *       .flatMap(row -&gt; orderRowResource.create(row));
     * </code>
     * </pre>
     *
     * @param list The list to run async operations on
     * @param <T>  The list and result item type
     * @return The asynchronous observable stream
     */
    public static <T> Observable<T> async(List<T> list) {
        return async(Observable.from(list));
    }

    /**
     * Converts an observable into an asynchronous observable stream.<br>
     * This is useful when multiple longer operations are involved that can run simultaneously.<br>
     * <br>
     * <b>Example usage:</b><br>
     * <pre>
     * <code>RxUtils.async(orderResource.create(order).map(order -&gt; order.getRows()))
     *       .flatMap(row -&gt; orderRowResource.create(row));
     * </code>
     * </pre>
     *
     * @param observable The observable to run async operations on
     * @param <T>        The observable and result item type
     * @return The asynchronous observable stream
     */
    public static <T> Observable<T> async(Observable<T> observable) {
        return Observable.merge(observable.nest());
    }

    /**
     * Sums together all double values in an observable.
     *
     * @param observable The observable to sum together
     * @return A observable containing the sum
     */
    public static Observable<Double> sum(Observable<Double> observable) {
        return observable.scan((valueA, valueB) -> valueA + valueB).lastOrDefault(0d);
    }

    /**
     * Execute an action if the stream is empty.
     *
     * @param output Stream to watch for entries.
     * @param action Action to take if the stream is empty.
     * @return The stream.
     */
    public static <T> Observable<T> doIfEmpty(Observable<T> output, Action0 action) {
        return output.doOnEach(new Observer<T>() {
            AtomicBoolean empty = new AtomicBoolean(true);

            @Override
            public void onCompleted() {
                if (empty.get()) {
                    action.call();
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onNext(T type) {
                empty.set(false);
            }
        });
    }

    public static <T> Observable<T> exception(Supplier<Exception> supplier) {
        return defer(() -> error(supplier.get()));
    }

    /**
     * Extracts a single item from an Observable of a collection of items. Returns an Observable if a single item if
     * the provided Observable of a collection of items emits a single list containing a single item. Returns empty if
     * the provided Observable of a collection of items is empty or if it emits a single list that contains no items.
     * Notifies of an {@code IllegalArgumentException} if the provided Observable of a collection of items either emits
     * more than one collection of items or if a single collection of items contains more than one item.
     *
     * @param collectionObservable Observable of a collection of items
     * @param <T> The observable and result item type
     * @return An Observable emitting a single item
     * @throws IllegalArgumentException if the provided Observable of a collection of items either emits more than one
     *              collection or if it emits a collection with more than one item
     */
    public static <T> Observable<T> singleOrEmpty(Observable<? extends Collection<T>> collectionObservable) {
        return collectionObservable.toList().concatMap(lists -> {
            if (lists.size() > 1) {
                return Observable.error(new IllegalArgumentException("Observable contains more than one collection."));
            } else if (lists.size() == 1 && lists.get(0).size() > 1) {
                return Observable.error(new IllegalArgumentException(
                    "Observable collection contains more than one item."));
            } else if (lists.size() == 1 && lists.get(0).size() == 1) {
                return Observable.just(lists.get(0).iterator().next());
            } else {
                return Observable.empty();
            }
        });
    }
}
