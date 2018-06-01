package se.fortnox.reactivewizard.validation;

import com.google.inject.Injector;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.server.ServerConfig;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static rx.Observable.just;

public class ResourceValidationTest {
    private Injector injector = TestInjector.create(binder -> {
        binder.bind(ServerConfig.class).toInstance(new ServerConfig(){{
            setEnabled(false);
        }});
    });
    private ValidatedResource validatedResource = injector.getInstance(ValidatedResource.class);

    @Test
    public void shouldGiveErrorForBadResourceCall() {
        try {
            validatedResource.acceptLong(10L).toBlocking().single();
            fail("expected exception");
        } catch (ValidationFailedException e) {
            assertThat(e.getFields()).hasSize(1);
            assertThat(e.getFields()[0].getField()).isEqualTo("queryParamValue");
        }
    }

    @Test
    public void shouldSucceedForCorrectResourceCall() {
        validatedResource.acceptLong(200L).toBlocking().single();
    }

    @Path("/")
    interface ValidatedResource {
        @GET
        Observable<String> acceptLong(@QueryParam("queryParamValue") @Min(100) Long value);
    }

    public static class ValidatedResourceImpl implements ValidatedResource {

        @Inject
        public ValidatedResourceImpl() {
        }

        @Override
        public Observable<String> acceptLong(@Min(100) Long value) {
            return just("hello");
        }
    }
}
