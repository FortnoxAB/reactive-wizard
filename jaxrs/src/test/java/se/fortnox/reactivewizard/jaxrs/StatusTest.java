package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.response.ResponseDecorator;
import se.fortnox.reactivewizard.mocks.MockHttpServerRequest;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import javax.ws.rs.*;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static rx.Observable.just;

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
        Observable<String> get();

        @Path("put")
        @PUT
        Observable<String> put();

        @Path("post")
        @POST
        Observable<String> post();

        @Path("patch")
        @PATCH
        Observable<String> patch();

        @Path("postWithQueryAndBody")
        @POST
        Observable<String> postWithQueryAndBody(@QueryParam("validInt") Integer validatedInt, ParamEntity param);

        @Path("delete")
        @DELETE
        Observable<Void> delete();

        @Path("post-custom")
        @POST
        @SuccessStatus(200)
        Observable<String> postCustom();

        @Path("get-content-disposition")
        @GET
        Observable<String> getContentDisposition();

        @Path("empty-redirect")
        @POST
        Observable<Void> emptyRedirect();

        @Path("empty-client-error")
        @POST
        Observable<Void> emptyClientError();

        @Path("empty-server-error")
        @POST
        Observable<Void> emptyServerError();

        @Path("empty-byte-array-body")
        @Produces("application/octet-stream")
        @GET
        Observable<byte[]> emptyByteArrayBody();

        @Path("empty-string-body")
        @Produces("text/plain")
        @GET
        Observable<String> emptyStringBody();
    }

    static class TestResourceImpl implements TestresourceInterface {

        @Override
        public Observable<String> get() {
            return just("get");
        }

        @Override
        public Observable<String> put() {
            return just("put");
        }

        @Override
        public Observable<String> post() {
            return just("post");
        }

        @Override
        public Observable<String> patch() {
            return just("patch");
        }

        @Override
        public Observable<Void> delete() {
            return Observable.empty();
        }

        @Override
        public Observable<String> postCustom() {
            return just("post-custom");
        }

        @Override
        public Observable<String> postWithQueryAndBody(Integer validatedInt, ParamEntity param) {
            return just("post ok");
        }

        @Override
        @Headers({"Content-Disposition: attachment; filename=\"export.csv\"", "test:test"})
        public Observable<String> getContentDisposition() {
            return just("content-disposition-ok");
        }

        @Override
        public Observable<Void> emptyRedirect() {
            return ResponseDecorator.of(Observable.<Void>empty())
                .withStatus(HttpResponseStatus.SEE_OTHER)
                .withHeader("Location", "/somewhere-else")
                .build();
        }

        @Override
        public Observable<Void> emptyClientError() {
            return ResponseDecorator.of(Observable.<Void>empty())
                .withStatus(HttpResponseStatus.GONE)
                .build();
        }

        @Override
        public Observable<Void> emptyServerError() {
            return ResponseDecorator.of(Observable.<Void>empty())
                .withStatus(HttpResponseStatus.SERVICE_UNAVAILABLE)
                .build();
        }

        @Override
        public Observable<byte[]> emptyByteArrayBody() {
            return just(new byte[0]);
        }

        @Override
        public Observable<String> emptyStringBody() {
            return just("");
        }
    }
}
