package se.fortnox.reactivewizard.db;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import static reactor.core.publisher.Flux.empty;
import static reactor.test.StepVerifier.create;

class DaoFluxSinkTest {

    private <T> Flux<T> daoMethod(boolean isMono, String methodName, Flux<T> databaseResults) {
        return Flux.create(fluxSink -> {
            DaoFluxSink<T> daoFluxSink = new DaoFluxSink<>(fluxSink, isMono, methodName);
            databaseResults.subscribeOn(Schedulers.immediate())
                .subscribe(daoFluxSink::next, daoFluxSink::error, daoFluxSink::complete);
        });
    }

    private <T> Flux<T> daoMethod(boolean isMono, Flux<T> databaseResults) {
        return daoMethod(isMono, "Unset method name", databaseResults);
    }

    @Test
    void shouldSignalErrorWhenAttemptingToEmitMultipleElementsOnAMonoDaoMethod() {
        create(daoMethod(true, "com.fortnox.users.v1.ValueDao::getAllValues", Flux.just("A", "B")))
            .verifyErrorMessage("com.fortnox.users.v1.ValueDao::getAllValues returning a Mono received more than one " +
                "result from the database");
    }

    @Test
    void shouldSignalNextAndCompleteOnAMonoDaoMethod() {
        create(daoMethod(true, Flux.just("A")))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldSignalErrorOnAMonoDaoMethod() {
        create(daoMethod(true, Flux.error(new Exception("Error message"))))
            .verifyErrorMessage("Error message");
    }

    @Test
    void shouldIgnoreErrorAfterFirstElementOnAMonoDaoMethod() {
        Flux<String> someValuesWithConnectionFailureError = Flux.create(fluxSink -> {
            fluxSink.next("A");
            fluxSink.error(new RuntimeException("This error will not be shown for a Mono since it has completed"));
        });

        create(daoMethod(true, someValuesWithConnectionFailureError))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldCompleteWhenEmptyOnAMonoDaoMethod() {
        create(daoMethod(true, empty())).verifyComplete();
    }

    @Test
    void shouldNextUntilCompleteOnAFluxDaoMethod() {
        create(daoMethod(false, Flux.just(1, 2, 3, 4, 5))).expectNextCount(5).verifyComplete();
    }

    @Test
    void shouldErrorAfterNextOnAFluxDaoMethod() {
        Flux<Integer> someDatabaseResults = Flux.create(fluxSink -> {
            fluxSink.next(1);
            fluxSink.error(new RuntimeException("Error message"));
        });

        create(daoMethod(false, someDatabaseResults)).expectNextCount(1).verifyErrorMessage("Error message");
    }

    @Test
    void shouldCompleteWhenEmptyOnAFluxDaoMethod() {
        create(daoMethod(false, Flux.empty())).verifyComplete();
    }

}
