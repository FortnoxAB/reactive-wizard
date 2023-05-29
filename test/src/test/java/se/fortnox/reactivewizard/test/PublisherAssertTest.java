package se.fortnox.reactivewizard.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.test.publisher.PublisherAssertions;

import static org.assertj.core.api.Assertions.assertThat;

class PublisherAssertTest {

    @Test
    void shouldPassContainsExactlyAssertionWhenSpecifiedValueIsExpected() {
        Mono<String> fluxUnderTest = Mono.just("one");
        PublisherAssertions.assertThat(fluxUnderTest)
            .containsExactly("one");
    }

    @Test
    void shouldPassContainsExactlyAssertionWhenSpecifiedValuesAreExpectedAndInOrder() {
        Flux<String> fluxUnderTest = Flux.just("one", "two", "three");
        PublisherAssertions.assertThat(fluxUnderTest)
            .containsExactly("one", "two", "three");
    }

    @Test
    void shouldFailContainsExactlyAssertionWhenSpecifiedValuesAreExpectedButNotInOrder() {
        Flux<String> fluxUnderTest = Flux.just("one", "two", "three");
        Assertions.assertThrows(AssertionError.class, () -> {
            PublisherAssertions.assertThat(fluxUnderTest).containsExactly("one", "three", "two");
        });
    }

    @Test
    void shouldFailContainsExactlyAssertionWhenValueIsUnexpected() {
        Mono<String> fluxUnderTest = Mono.just("one");
        Assertions.assertThrows(AssertionError.class, () -> {
            PublisherAssertions.assertThat(fluxUnderTest)
                .containsExactly("two");
        });
    }

    @Test
    void shouldFailContainsExactlyAssertionWhenMoreThanOneValue() {
        Flux<String> fluxUnderTest = Flux.just("one", "two");
        Assertions.assertThrows(AssertionError.class, () -> {
            PublisherAssertions.assertThat(fluxUnderTest)
                .containsExactly("one");
        });
    }

    @Test
    void shouldFailContainsExactlyAssertionWhenEmpty() {
        Mono<String> fluxUnderTest = Mono.empty();
        Assertions.assertThrows(AssertionError.class, () -> {
            PublisherAssertions.assertThat(fluxUnderTest)
                .containsExactly("one");
        });
    }

    @Test
    void shouldReturnAssertionForOneValue() {
        Mono<String> fluxUnderTest = Mono.just("one");
        PublisherAssertions.assertThat(fluxUnderTest)
            .singleElement()
            .satisfies(value -> assertThat(value)
                .isEqualTo("one"));
    }

    @Test
    void shouldFailToReturnAssertionForOneValueWhenMoreThanOne() {
        Flux<String> fluxUnderTest = Flux.just("one", "two");
        Assertions.assertThrows(AssertionError.class, () -> {
            PublisherAssertions.assertThat(fluxUnderTest)
                .singleElement()
                .satisfies(value -> assertThat(value)
                    .isEqualTo("one"));
        });
    }

    @Test
    void shouldPassWhenEmptyMonoIsExpectedToBeEmpty() {
        Mono<String> fluxUnderTest = Mono.empty();
        PublisherAssertions.assertThat(fluxUnderTest)
            .isEmpty();
    }

    @Test
    void shouldFailWhenNonEmptyMonoIsExpectedToBeEmpty() {
        Mono<String> fluxUnderTest = Mono.just("one");
        Assertions.assertThrows(AssertionError.class, () -> {
            PublisherAssertions.assertThat(fluxUnderTest)
                .isEmpty();
        });
    }
}
