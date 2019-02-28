package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import org.junit.Test;
import rx.Observable;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static rx.Observable.just;

public class WrappedParamTest {

    @Test
    public void shouldUseSubclassOfParamIfWrapAnnotationUsed() {
        JaxRsRequestHandler handler = createHandler(new TestApi() {

            @Override
            public Observable<String> getType(@Wrap(EntitySubClass.class) Entity param) {
                return just(param.getClass().getName());
            }
        });
        MockHttpServerRequest  request = new MockHttpServerRequest("/", HttpMethod.POST, "{}");
        MockHttpServerResponse resp    = runRequest(handler, request);
        assertThat(resp.getOutp()).isEqualTo("\"" + EntitySubClass.class.getName() + "\"");
    }

    @Test
    public void shouldReturnErrorWhenWrapIsNotSubclassOfApiParam() {
        try {
            createHandler(new TestApi() {
                @Override
                public Observable<String> getType(@Wrap(String.class) Entity param) {
                    return just(param.getClass().getName());
                }
            });
            fail("expected exception");
        } catch (Exception e) {
            String expected = "Wrapper for public rx.Observable se.fortnox.reactivewizard.jaxrs.WrappedParamTest$2.getType" +
                "(se.fortnox.reactivewizard.jaxrs.Entity) not correct. " +
                "class java.lang.String must be subclass of class se.fortnox.reactivewizard.jaxrs.Entity";
            assertThat(e.getMessage()).isEqualTo(expected);
            return;
        }
        fail("expected exception");
    }

    private MockHttpServerResponse runRequest(JaxRsRequestHandler handler, MockHttpServerRequest req) {
        MockHttpServerResponse resp = new MockHttpServerResponse();
        handler.handle(req, resp).isEmpty().toBlocking().single();
        return resp;
    }

    private JaxRsRequestHandler createHandler(Object service) {
        return new JaxRsRequestHandler(
                new Object[]{service},
                new JaxRsResourceFactory(),
                new ExceptionHandler(),
                new ByteBufCollector(),
                false
        );
    }

    @Path("")
    interface TestApi {
        @POST
        Observable<String> getType(Entity param);
    }

}

class Entity {

}

class EntitySubClass extends Entity {
    public EntitySubClass() {

    }
}

