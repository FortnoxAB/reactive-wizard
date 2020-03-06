package se.fortnox.reactivewizard.jaxrs;

import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.jaxrs.context.JaxRsRequestContext;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;

public class JaxRsRequestHandlerContextTest {
    private JaxRsRequestHandler handler;

    @Before
    public void setUp() {
        handler = new JaxRsRequestHandler(new Service());
    }

    @Test
    public void shouldPropagateContextThroughObservables() {
        MockHttpServerResponse response = new MockHttpServerResponse();
        handler.handle(new MockHttpServerRequest("/"), response).toBlocking().subscribe();

        // The context should be closed as soon as .handle(...) exits
        assertThat(JaxRsRequestContext.getValue("key").isPresent()).isFalse();

        // The response is gotten from the context during the execution of the observable
        assertThat(response.getOutp()).isEqualTo("\"value\"");
    }

    @Path("/")
    public static class Service {
        @GET
        public Observable<String> foo() {
            // The context is opened in the handler right before this is called (in the same thread)
            JaxRsRequestContext.setValue("key", "value");

            // When an observable is created, the current open context is attached to the observable
            // (see hooks in RxJava1ContextPropagator in the reactive-contexts library)
            return Observable.just("")
                // Force a thread switch when executing the observable to make sure the context is still available
                .delay(10, TimeUnit.MILLISECONDS)
                // Due to the delay, this is executed on RxComputationScheduler instead of the main thread and the
                // context should still be available
                .map(o -> JaxRsRequestContext.<String>getValue("key").orElse(null));
        }
    }
}
