package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.jaxrs.response.ResponseDecorator;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;
import se.fortnox.reactivewizard.util.ReactiveDecorator;
import se.fortnox.reactivewizard.utils.JaxRsTestUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static rx.Observable.defer;
import static rx.Observable.just;

public class ResponseDecoratorTest {

    @Test
    public void shouldReturnHeaderFromResource() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/headers");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("value");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    public void shouldReturnDeferredHeaderFromResource() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/headers/deferred");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("deferred");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    public void shouldReturnHeaderFromResourceWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/with-builder/headers");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("value");
        assertThat(response.responseHeaders().get("custom_header2")).isEqualTo("value2");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    public void shouldReturnDeferredHeaderFromResourceWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/with-builder/headers/deferred");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("deferred");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    public void shouldReturnStatusFromResourceWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/with-builder/status");
        assertThat(response.status()).isEqualTo(HttpResponseStatus.MOVED_PERMANENTLY);
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    public void shouldReturnDeferredStatusFromResourceWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/with-builder/status/deferred");
        assertThat(response.status()).isEqualTo(HttpResponseStatus.MOVED_PERMANENTLY);
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    public void shouldNotFailIfDecorationIsAtomicReference() {
        try {
            MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/atomicreference");
            assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);

        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Path("/")
    public class ResourceWithHeaders {
        @GET
        @Path("headers")
        public Observable<String> methodReturningHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("custom_header", "value");
            return ResponseDecorator.withHeaders(just("body"), headers);
        }

        @GET
        @Path("atomicreference")
        public Observable<String> methodReturningDecoratedWithAtomicReference() {
            return ReactiveDecorator.decorated(just(""), new AtomicReference<>());
        }

        @GET
        @Path("headers/deferred")
        public Observable<String> methodReturningDeferredHeaders() {
            Map<String, String> headers = new HashMap<>();
            return ResponseDecorator.withHeaders(defer(() -> {
                headers.put("custom_header", "deferred");
                return just("body");
            }), headers);
        }

        @GET
        @Path("with-builder/headers")
        public Observable<String> methodReturningHeadersWithBuilder() {
            Map<String, String> headers = new HashMap<>();
            headers.put("custom_header", "value");
            return ResponseDecorator.of(just("body"))
                .withHeaders(headers)
                .withHeader("custom_header2", "value2")
                .build();
        }

        @GET
        @Path("with-builder/headers/deferred")
        public Observable<String> methodReturningDeferredHeadersWithBuilder() {
            Map<String, String> headers = new HashMap<>();
            return ResponseDecorator.of(defer(() -> {
                headers.put("custom_header", "deferred");
                return just("body");
            }))
                .withHeaders(headers)
                .build();
        }

        @GET
        @Path("with-builder/status")
        public Observable<String> methodReturningStatusWithBuilder() {
            return ResponseDecorator.of(just("body"))
                .withStatus(HttpResponseStatus.MOVED_PERMANENTLY)
                .build();
        }

        @GET
        @Path("with-builder/status/deferred")
        public Observable<String> methodReturningDeferredStatusWithBuilder() {
            AtomicReference<HttpResponseStatus> status = new AtomicReference<>(HttpResponseStatus.OK);
            return ResponseDecorator.of(defer(() -> {
                status.set(HttpResponseStatus.MOVED_PERMANENTLY);
                return just("body");
            }))
                .withStatus(status)
                .build();
        }
    }
}
