package se.fortnox.reactivewizard.jaxrs;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.mocks.MockHttpServerRequest;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static java.util.Collections.singleton;
import static org.fest.assertions.Assertions.assertThat;
import static rx.Observable.just;

public class JaxRsRequestHandlerTest {
    private JaxRsRequestHandler handler;
    private Interceptor interceptor;

    @Before
    public void setUp() {
        interceptor = new Interceptor();
        handler = new JaxRsRequestHandler(new Object[] { new Service() },
            new JaxRsResourceFactory(),
            new ExceptionHandler(),
            new ByteBufCollector(),
            null,
            new JaxRsResourceInterceptors(singleton(interceptor)));
    }

    @Test
    public void shouldInterceptResourceCall() {
        MockHttpServerRequest request = new MockHttpServerRequest("/");
        MockHttpServerResponse response = new MockHttpServerResponse();
        Publisher<Void> result = handler.apply(request, response);

        assertThat(interceptor.preHandled).isNotNull();
        assertThat(interceptor.postHandled).isNotNull();
        assertThat(interceptor.resourceCall).isSameAs(result);
    }

    private static class Interceptor implements JaxRsResourceInterceptor {
        private JaxRsResourceContext preHandled;
        private JaxRsResourceContext postHandled;
        private Publisher<Void>     resourceCall;

        @Override
        public void preHandle(JaxRsResourceContext context) {
            preHandled = context;
        }

        @Override
        public void postHandle(JaxRsResourceContext context, Publisher<Void> resourceCall) {
            postHandled = context;
            this.resourceCall = resourceCall;
        }
    }

    @Path("/")
    private static class Service {
        @GET
        public Observable<String> foo() {
            return just("foo");
        }
    }
}
