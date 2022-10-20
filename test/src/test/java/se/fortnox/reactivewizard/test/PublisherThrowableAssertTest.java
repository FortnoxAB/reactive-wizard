package se.fortnox.reactivewizard.test;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.test.publisher.PublisherAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class PublisherThrowableAssertTest {

    @Test
    public void shouldPassWhenExpectedExceptionIsThrown() {
        IllegalArgumentException exception = new IllegalArgumentException("Error");
        Flux<String> fluxUnderTest = Flux.error(exception);
        PublisherAssertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isEmittedBy(fluxUnderTest)
            .satisfies(illegalArgumentException ->
                assertThat(illegalArgumentException).isEqualTo(exception));
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenExceptionOfUnexpectedTypeIsThrown() {
        IllegalArgumentException exception = new IllegalArgumentException("Error");
        Mono<String> fluxUnderTest = Mono.error(exception);
        PublisherAssertions.assertThatExceptionOfType(IllegalStateException.class)
            .isEmittedBy(fluxUnderTest);
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenExceptionIsExpectedButNotThrown() {
        Mono<String> fluxUnderTest = Mono.just("one");
        PublisherAssertions.assertThatExceptionOfType(IllegalStateException.class)
            .isEmittedBy(fluxUnderTest);
    }
}
