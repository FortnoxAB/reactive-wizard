package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.mocks.MockHttpServerRequest;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class WrappedParamTest {

    @Test
    public void shouldUseSubclassOfParamIfWrapAnnotationUsed() {
        JaxRsRequestHandler handler = createHandler(new TestApi() {

            @Override
            public Mono<String> getType(@Wrap(EntitySubClass.class) Entity param) {
                return Mono.just(param.getClass().getName());
            }
        });
        HttpServerRequest request = new MockHttpServerRequest("/", HttpMethod.POST, "{}");
        MockHttpServerResponse resp    = runRequest(handler, request);
        assertThat(resp.getOutp()).isEqualTo("\"" + EntitySubClass.class.getName() + "\"");
    }

    @Test
    public void shouldReturnErrorWhenWrapIsNotSubclassOfApiParam() {
        try {
            createHandler(new TestApi() {
                @Override
                public Mono<String> getType(@Wrap(String.class) Entity param) {
                    return Mono.just(param.getClass().getName());
                }
            });
            fail("expected exception");
        } catch (Exception e) {
            String expected = "Wrapper for public reactor.core.publisher.Mono se.fortnox.reactivewizard.jaxrs.WrappedParamTest$2.getType" +
                "(se.fortnox.reactivewizard.jaxrs.Entity) not correct. " +
                "class java.lang.String must be subclass of class se.fortnox.reactivewizard.jaxrs.Entity";
            assertThat(e.getMessage()).isEqualTo(expected);
            return;
        }
        fail("expected exception");
    }

    private MockHttpServerResponse runRequest(JaxRsRequestHandler handler, HttpServerRequest req) {
        MockHttpServerResponse resp = new MockHttpServerResponse();
        Flux.from(handler.apply(req, resp)).count().block();
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
        Mono<String> getType(Entity param);
    }

}

class Entity {

}

class EntitySubClass extends Entity {
    public EntitySubClass() {

    }
}

