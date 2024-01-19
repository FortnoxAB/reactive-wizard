package se.fortnox.reactivewizard.validation;

import com.google.inject.Injector;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.server.ServerConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ResourceValidationTest {
    private final Injector injector = TestInjector.create(binder -> binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
        setEnabled(false);
    }}));
    private final ValidatedResource validatedResource = injector.getInstance(ValidatedResource.class);

    @Test
    void shouldGiveErrorForBadResourceCall() {
        try {
            validatedResource.fluxAcceptLong(10L).blockLast();
            validatedResource.monoAcceptLong(10L).block();
            fail("expected exception");
        } catch (ValidationFailedException e) {
            assertThat(e.getFields()).hasSize(1);
            assertThat(e.getFields()[0].getField()).isEqualTo("queryParamValue");
        }
    }

    @Test
    void shouldSucceedForCorrectResourceCall() {
        StepVerifier.create(Flux.merge(validatedResource.fluxAcceptLong(101L), validatedResource.monoAcceptLong(101L)))
            .expectNextCount(2)
            .verifyComplete();
    }

    @Path("/")
    interface ValidatedResource {
        @GET
        Flux<String> fluxAcceptLong(@QueryParam("queryParamValue") @Min(100) Long value);

        @GET
        Mono<String> monoAcceptLong(@QueryParam("queryParamValue") @Min(100) Long value);
    }

    public static class ValidatedResourceImpl implements ValidatedResource {

        @Inject
        public ValidatedResourceImpl() {
        }

        @Override
        public Flux<String> fluxAcceptLong(Long value) {
            return Flux.just("hello");
        }

        @Override
        public Mono<String> monoAcceptLong(Long value) {
            return Mono.just("hello");
        }
    }
}
