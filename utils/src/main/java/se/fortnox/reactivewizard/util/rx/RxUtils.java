package se.fortnox.reactivewizard.util.rx;

import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action2;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static rx.Observable.defer;
import static rx.Observable.error;

public class RxUtils {

	public static FirstThen first(Observable<?> doFirst) {
		return new FirstThen(doFirst);
	}

	public static <T> IfThenElse<T> ifTrue(Observable<Boolean> ifValue) {
		return new IfThenElse<T>(ifValue);
	}

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
	 * @param <T> The list and result item type
     * @return The asynchronous observable stream
     */
	public static <T> Observable<T> async(List<T> list){
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
	 * @param <T> The observable and result item type
     * @return The asynchronous observable stream
     */
	public static <T> Observable<T> async(Observable<T> observable){
		return Observable.merge(observable.nest());
	}

	/**
	 * Sums together all double values in an observable.
	 *
	 * @param observable The observable to sum together
	 * @return A observable containing the sum
     */
	public static Observable<Double> sum(Observable<Double> observable) {
		return observable.scan((a, b) -> a + b).lastOrDefault(0d);
	}

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
			public void onNext(T t) {
				empty.set(false);
			}
		});
	}

	public static <T> Observable<T> exception(Supplier<Exception> supplier) {
		return defer(() -> error(supplier.get()));
	}
}
