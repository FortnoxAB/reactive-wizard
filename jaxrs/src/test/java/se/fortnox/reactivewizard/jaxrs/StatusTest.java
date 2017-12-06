package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.MockHttpServerResponse;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static org.fest.assertions.Assertions.assertThat;
import static rx.Observable.just;

public class StatusTest {

    ExceptionHandler    exceptionHandler = new ExceptionHandler();
    JaxRsRequestHandler handler          = new JaxRsRequestHandler(new Object[]{new TestresourceImpl()},
        new JaxRsResourceFactory(),
        exceptionHandler,
        false);

    @Test
    public void shouldReturn200ForGetPutPatch() {
        assertStatus("/test/get", HttpMethod.GET, HttpResponseStatus.OK);
        assertStatus("/test/put", HttpMethod.PUT, HttpResponseStatus.OK);
        assertStatus("/test/patch", HttpMethod.PATCH, HttpResponseStatus.OK);
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
        HttpServerRequest<ByteBuf> request  = new MockHttpServerRequest("/test/delete", HttpMethod.DELETE);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Observable<Void>           result   = handler.handle(request, response);
        result.onErrorReturn(e -> {
            exceptionHandler.handleException(request, response, e);
            return null;
        }).toBlocking().singleOrDefault(null);
        assertThat(response.getStatus()).isEqualTo(HttpResponseStatus.NO_CONTENT);
        assertThat(response.getHeader("Content-Length")).isEqualTo("0"); // Will be removed in pipeline filter
        assertThat(response.getHeader("Transfer-Encoding")).isNull();
        assertThat(response.getOutp()).isEmpty();
    }

    @Test
    public void shouldReturnContentDispositionHeaderIfPresent() {
        HttpServerRequest<ByteBuf> request  = new MockHttpServerRequest("/test/get-content-disposition", HttpMethod.GET);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Observable<Void>           result   = handler.handle(request, response);
        result.onErrorReturn(e -> {
            exceptionHandler.handleException(request, response, e);
            return null;
        }).toBlocking().singleOrDefault(null);

        assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment; filename=\"export.csv\"");
        assertThat(response.getHeader("test")).isEqualTo("test");
    }

    @Test
    public void shouldReturnNoContentDispositionHeaderIfAbsent() {
        HttpServerRequest<ByteBuf> request  = new MockHttpServerRequest("/test/get", HttpMethod.GET);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Observable<Void>           result   = handler.handle(request, response);
        result.onErrorReturn(e -> {
            exceptionHandler.handleException(request, response, e);
            return null;
        }).toBlocking().singleOrDefault(null);

        assertThat(response.getHeader("Content-Disposition")).isNull();
    }

    @Test
    public void returnsGivenStatusWithSuccessStatusAnnotation() {
        assertStatus("/test/post-custom", HttpMethod.POST, HttpResponseStatus.OK);
    }

    private void assertStatus(String url, HttpMethod httpMethod, HttpResponseStatus status) {
        assertStatus(url, httpMethod, status, null);
    }

    private void assertStatus(String url, HttpMethod httpMethod, HttpResponseStatus status, String body) {
        HttpServerRequest<ByteBuf> request  = new MockHttpServerRequest(url, httpMethod, body);
        MockHttpServerResponse     response = new MockHttpServerResponse();
        Observable<Void>           result   = handler.handle(request, response);
        result.onErrorReturn(e -> {
            exceptionHandler.handleException(request, response, e);
            return null;
        }).toBlocking().singleOrDefault(null);
        assertThat(response.getStatus()).isEqualTo(status);
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
    }

    class TestresourceImpl implements TestresourceInterface {

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
    }
}
