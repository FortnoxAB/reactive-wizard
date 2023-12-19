package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.response.ResponseDecorator;
import se.fortnox.reactivewizard.mocks.MockHttpServerRequest;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static reactor.core.publisher.Mono.just;

public class StatusTest {

    ExceptionHandler    exceptionHandler = new ExceptionHandler();
    JaxRsRequestHandler handler          = new JaxRsRequestHandler(
            new Object[]{new TestResourceImpl()},
            new JaxRsResourceFactory(),
            exceptionHandler,
            new ByteBufCollector(),
            false
    );

    @ParameterizedTest
    @MethodSource("httpPathAndMethodProvider")
    public void shouldReturn200ForGetPutPatch(String path, HttpMethod httpMethod) {
        assertStatus(path, httpMethod, HttpResponseStatus.OK);
    }

    private static Stream<Arguments> httpPathAndMethodProvider() {
        return Stream.of(
            Arguments.arguments("/test/get", HttpMethod.GET),
            Arguments.arguments("/test/put", HttpMethod.PUT),
            Arguments.arguments("/test/patch", HttpMethod.PATCH)
        );
    }

    @Test
    public void shouldReturn201ForPost() {
        assertStatus("/test/post", HttpMethod.POST, HttpResponseStatus.CREATED);
        assertStatus("/test/postWithQueryAndBody",
            HttpMethod.POST,
            HttpResponseStatus.CREATED,
            "{\"name\":\"hej\"}");
    }

    @Test
    public void shouldReturn204ForDelete() {
        assertStatus("/test/delete", HttpMethod.DELETE, HttpResponseStatus.NO_CONTENT);
    }

    @Test
    public void shouldReturnEmptyResponseWith204Status() {
        MockHttpServerRequest      request  = new MockHttpServerRequest("/test/delete", HttpMethod.DELETE);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Flux<Void>                 result   = Flux.from(handler.apply(request, response));
        result.onErrorResume(e -> {
            exceptionHandler.handleException(request, response, e);
            return Flux.empty();
        }).count().block();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.NO_CONTENT);
        assertThat(response.responseHeaders().get("Content-Length")).isEqualTo("0"); // Will be removed in pipeline filter
        assertThat(response.responseHeaders().get("Transfer-Encoding")).isNull();
        assertThat(response.getOutp()).isEmpty();
    }

    @Test
    public void shouldReturnEmptyByteBodyWith204Status() {
        MockHttpServerRequest      request  = new MockHttpServerRequest("/test/empty-byte-array-body", HttpMethod.GET);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Flux<Void>                 result   = Flux.from(handler.apply(request, response));
        result.onErrorResume(e -> {
            exceptionHandler.handleException(request, response, e);
            return Flux.empty();
        }).count().block();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.NO_CONTENT);
        assertThat(response.responseHeaders().get("Content-Length")).isEqualTo("0"); // Will be removed in pipeline filter
        assertThat(response.responseHeaders().get("Transfer-Encoding")).isNull();
        assertThat(response.getOutp()).isEmpty();
    }

    @Test
    public void shouldReturnEmptyStringBodyWith204Status() {
        MockHttpServerRequest      request  = new MockHttpServerRequest("/test/empty-string-body", HttpMethod.GET);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Flux<Void>                 result   = Flux.from(handler.apply(request, response));
        result.onErrorResume(e -> {
            exceptionHandler.handleException(request, response, e);
            return Flux.empty();
        }).count().block();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.NO_CONTENT);
        assertThat(response.responseHeaders().get("Content-Length")).isEqualTo("0"); // Will be removed in pipeline filter
        assertThat(response.responseHeaders().get("Transfer-Encoding")).isNull();
        assertThat(response.getOutp()).isEmpty();
    }

    @Test
    public void shouldReturnContentDispositionHeaderIfPresent() {
        MockHttpServerRequest      request  = new MockHttpServerRequest("/test/get-content-disposition", HttpMethod.GET);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Flux<Void>                 result   = Flux.from(handler.apply(request, response));
        result.onErrorResume(e -> {
            exceptionHandler.handleException(request, response, e);
            return Flux.empty();
        }).count().block();

        assertThat(response.responseHeaders().get("Content-Disposition")).isEqualTo("attachment; filename=\"export.csv\"");
        assertThat(response.responseHeaders().get("test")).isEqualTo("test");
    }

    @Test
    public void shouldReturnNoContentDispositionHeaderIfAbsent() {
        MockHttpServerRequest      request  = new MockHttpServerRequest("/test/get", HttpMethod.GET);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Flux<Void>                 result   = Flux.from(handler.apply(request, response));
        result.onErrorResume(e -> {
            exceptionHandler.handleException(request, response, e);
            return Flux.empty();
        }).count().block();

        assertThat(response.responseHeaders().get("Content-Disposition")).isNull();
    }

    @Test
    public void returnsGivenStatusWithSuccessStatusAnnotation() {
        assertStatus("/test/post-custom", HttpMethod.POST, HttpResponseStatus.OK);
    }

    @Test
    public void shouldNotModifyStatusOnEmptyRedirects() {
        assertStatus("/test/empty-redirect", HttpMethod.POST, HttpResponseStatus.SEE_OTHER);
    }

    @Test
    public void shouldNotModifyStatusOnEmptyClientErrors() {
        assertStatus("/test/empty-client-error", HttpMethod.POST, HttpResponseStatus.GONE);
    }

    @Test
    public void shouldNotModifyStatusOnEmptyServerErrors() {
        assertStatus("/test/empty-server-error", HttpMethod.POST, HttpResponseStatus.SERVICE_UNAVAILABLE);
    }

    private void assertStatus(String url, HttpMethod httpMethod, HttpResponseStatus status) {
        assertStatus(url, httpMethod, status, null);
    }

    private void assertStatus(String url, HttpMethod httpMethod, HttpResponseStatus status, String body) {
        MockHttpServerRequest      request  = new MockHttpServerRequest(url, httpMethod, body);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Flux<Void>                 result   = Flux.from(handler.apply(request, response));
        result.onErrorResume(e -> {
            exceptionHandler.handleException(request, response, e);
            return Flux.empty();
        }).count().block();
        assertThat(response.status()).isEqualTo(status);
    }

    @Path("test")
    public interface TestresourceInterface {
        @Path("get")
        @GET
        Mono<String> get();

        @Path("put")
        @PUT
        Mono<String> put();

        @Path("post")
        @POST
        Mono<String> post();

        @Path("patch")
        @PATCH
        Mono<String> patch();

        @Path("postWithQueryAndBody")
        @POST
        Mono<String> postWithQueryAndBody(@QueryParam("validInt") Integer validatedInt, ParamEntity param);

        @Path("delete")
        @DELETE
        Mono<Void> delete();

        @Path("post-custom")
        @POST
        @SuccessStatus(200)
        Mono<String> postCustom();

        @Path("get-content-disposition")
        @GET
        Mono<String> getContentDisposition();

        @Path("empty-redirect")
        @POST
        Mono<Void> emptyRedirect();

        @Path("empty-client-error")
        @POST
        Mono<Void> emptyClientError();

        @Path("empty-server-error")
        @POST
        Mono<Void> emptyServerError();

        @Path("empty-byte-array-body")
        @Produces("application/octet-stream")
        @GET
        Flux<byte[]> emptyByteArrayBody();

        @Path("empty-string-body")
        @Produces("text/plain")
        @GET
        Mono<String> emptyStringBody();
    }

    static class TestResourceImpl implements TestresourceInterface {

        @Override
        public Mono<String> get() {
            return just("get");
        }

        @Override
        public Mono<String> put() {
            return just("put");
        }

        @Override
        public Mono<String> post() {
            return just("post");
        }

        @Override
        public Mono<String> patch() {
            return just("patch");
        }

        @Override
        public Mono<Void> delete() {
            return Mono.empty();
        }

        @Override
        public Mono<String> postCustom() {
            return just("post-custom");
        }

        @Override
        public Mono<String> postWithQueryAndBody(Integer validatedInt, ParamEntity param) {
            return just("post ok");
        }

        @Override
        @Headers({"Content-Disposition: attachment; filename=\"export.csv\"", "test:test"})
        public Mono<String> getContentDisposition() {
            return just("content-disposition-ok");
        }

        @Override
        public Mono<Void> emptyRedirect() {
            return ResponseDecorator.of(Mono.<Void>empty())
                .withStatus(HttpResponseStatus.SEE_OTHER)
                .withHeader("Location", "/somewhere-else")
                .build();
        }

        @Override
        public Mono<Void> emptyClientError() {
            return ResponseDecorator.of(Mono.<Void>empty())
                .withStatus(HttpResponseStatus.GONE)
                .build();
        }

        @Override
        public Mono<Void> emptyServerError() {
            return ResponseDecorator.of(Mono.<Void>empty())
                .withStatus(HttpResponseStatus.SERVICE_UNAVAILABLE)
                .build();
        }

        @Override
        public Flux<byte[]> emptyByteArrayBody() {
            return Flux.just(new byte[0]);
        }

        @Override
        public Mono<String> emptyStringBody() {
            return just("");
        }
    }
}
