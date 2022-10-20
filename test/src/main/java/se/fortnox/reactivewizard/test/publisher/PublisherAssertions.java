package se.fortnox.reactivewizard.test.publisher;

import org.reactivestreams.Publisher;

/**
 * Entry point for assertion methods to make it more convenient to user AssertJ assertions with Publisher.
 * Each method in this class is a static factory for assertion objects.
 * <p>
 * Examples:
 * <pre><code class='java'>
 * Flux&lt;List&lt;Long&gt;&lt; flux = Flux.just(List.of(1L, 2L));
 * FluxAssertions.assertThat(flux)
 *    .singleElement()
 *    .satisfies(listOfLongs -&gt;
 *       Assertions.assertThat(listOfLongs)
 *          .contains(1l, 2L)
 *    );
 * </code></pre>
 *
 * <pre><code class='java'>
 * Flux&lt;String&lt; flux = Flux.just("one");
 * PublisherAssertions.assertThat(flux)
 *   .containsExactly("one");
 * </code></pre>
 *
 * <pre><code class='java'>
 * Flux&lt;String&lt; flux = Flux.just("one", "two");
 * FluxAssertions.assertThat(flux)
 *   .containsExactly("one","two");
 * </code></pre>
 */
public class PublisherAssertions {

    private PublisherAssertions() {
    }

    /**
     * Creates a new instance of <code>{@link PublisherAssert}</code>.
     *
     * @param <T>    the type of elements emitted by the Publisher.
     * @param actual the Publisher under test.
     * @return the created assertion object.
     */
    public static <T> PublisherAssert<T> assertThat(Publisher<T> actual) {
        return new PublisherAssert<>(actual);
    }

    /**
     * Entry point to check that one onError signal with an exception of type T was received by a given Publisher,
     * which allows to chain assertions on the thrown exception.
     * <p>
     * Example:
     * <pre><code class='java'> assertThatExceptionOfType(IOException.class)
     *           .isThrownBy(publisher)
     *           .withMessage("boom!"); </code></pre>
     *
     * @param <T>           the exception type.
     * @param exceptionType the exception type class.
     * @return the created {@link PublisherThrowableAssert}.
     */
    public static <T extends Throwable> PublisherThrowableAssert<T> assertThatExceptionOfType(final Class<? extends T> exceptionType) {
        return new PublisherThrowableAssert<>(exceptionType);
    }
}
