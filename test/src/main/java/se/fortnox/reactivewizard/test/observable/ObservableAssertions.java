package se.fortnox.reactivewizard.test.observable;

import rx.Observable;

/**
 * Entry point for assertion methods to make it more convenient to user AssertJ assertions with Observables.
 * Each method in this class is a static factory for assertion objects.
 * <p>
 * Examples:
 * <pre><code class='java'>
 * Observable&lt;List&lt;Long&gt;&lt; observable = Observable.just(List.of(1L, 2L));
 * ObservableAssertions.assertThat(observable)
 *    .singleElement()
 *    .satisfies(listOfLongs -&gt;
 *       Assertions.assertThat(listOfLongs)
 *          .contains(1l, 2L)
 *    );
 * </code></pre>
 *
 * <pre><code class='java'>
 * Observable&lt;String&lt; observable = Observable.just("one");
 * ObservableAssertions.assertThat(observable)
 *   .containsExactly("one");
 * </code></pre>
 *
 * <pre><code class='java'>
 * Observable&lt;String&lt; observable = Observable.just("one", "two");
 * ObservableAssertions.assertThat(observable)
 *   .containsExactly("one","two");
 * </code></pre>
 */
public class ObservableAssertions {

    private ObservableAssertions() {
    }

    /**
     * Creates a new instance of <code>{@link ObservableAssert}</code>.
     *
     * @param <T>    the type of elements emitted by the Observable.
     * @param actual the Observable under test.
     * @return the created assertion object.
     */
    public static <T> ObservableAssert<T> assertThat(Observable<T> actual) {
        return new ObservableAssert<>(actual);
    }

    /**
     * Entry point to check that one onError signal with an exception of type T was received by a given Observable,
     * which allows to chain assertions on the thrown exception.
     * <p>
     * Example:
     * <pre><code class='java'> assertThatExceptionOfType(IOException.class)
     *           .isThrownBy(observable)
     *           .withMessage("boom!"); </code></pre>
     *
     * @param <T>           the exception type.
     * @param exceptionType the exception type class.
     * @return the created {@link ObservableThrowableAssert}.
     */
    public static <T extends Throwable> ObservableThrowableAssert<T> assertThatExceptionOfType(final Class<? extends T> exceptionType) {
        return new ObservableThrowableAssert<>(exceptionType);
    }
}
