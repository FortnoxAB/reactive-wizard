package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.jaxrs.response.ResponseDecorator;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;
import se.fortnox.reactivewizard.util.ReactiveDecorator;
import se.fortnox.reactivewizard.utils.JaxRsTestUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

class ResponseDecoratorTest {

    @Test
    void shouldReturnHeaderFromResource() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/headers");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("value");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    void shouldReturnHeadersFromResourceWithEmptyBody() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/headers/empty-body");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("value");
        assertThat(response.getOutp()).isEqualTo("");
    }

    @Test
    void shouldReturnDeferredHeaderFromResource() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/headers/deferred");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("deferred");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    void shouldReturnHeaderFromResourceWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/with-builder/headers");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("value");
        assertThat(response.responseHeaders().get("custom_header2")).isEqualTo("value2");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    void shouldReturnHeadersFromResourceWithEmptyBodyWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "with-builder/headers/empty-body");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("value");
        assertThat(response.getOutp()).isEqualTo("");
    }

    @Test
    void shouldReturnDeferredHeaderFromResourceWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/with-builder/headers/deferred");
        assertThat(response.responseHeaders().get("custom_header")).isEqualTo("deferred");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    void shouldReturnStatusFromResourceWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/with-builder/status");
        assertThat(response.status()).isEqualTo(HttpResponseStatus.MOVED_PERMANENTLY);
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    void shouldReturnStatusFromResourceWithEmptyWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/with-builder/status/empty");
        assertThat(response.status()).isEqualTo(HttpResponseStatus.MOVED_PERMANENTLY);
        assertThat(response.getOutp()).isEqualTo("");
    }

    @Test
    void shouldReturnDeferredStatusFromResourceWithBuilder() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/with-builder/status/deferred");
        assertThat(response.status()).isEqualTo(HttpResponseStatus.MOVED_PERMANENTLY);
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    void shouldNotFailIfDecorationIsAtomicReference() {
        try {
            MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/atomicreference");
            assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);

        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Path("/")
    public static class ResourceWithHeaders {
        @GET
        @Path("headers")
        public Mono<String> methodReturningHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("custom_header", "value");
            return ResponseDecorator.withHeaders(just("body"), headers);
        }

        @GET
        @Path("headers/empty-body")
        public Mono<Void> methodReturningWithHeadersWithEmptyBody() {
            return ResponseDecorator.withHeaders(empty(), Map.of("custom_header", "value"));
        }

        @GET
        @Path("atomicreference")
        public Mono<String> methodReturningDecoratedWithAtomicReference() {
            return ReactiveDecorator.decorated(just(""), new AtomicReference<>());
        }

        @GET
        @Path("headers/deferred")
        public Mono<String> methodReturningDeferredHeaders() {
            Map<String, String> headers = new HashMap<>();
            return ResponseDecorator.withHeaders(defer(() -> {
                headers.put("custom_header", "deferred");
                return just("body");
            }), headers);
        }

        @GET
        @Path("with-builder/headers")
        public Mono<String> methodReturningHeadersWithBuilder() {
            Map<String, String> headers = new HashMap<>();
            headers.put("custom_header", "value");
            return ResponseDecorator.of(just("body"))
                .withHeaders(headers)
                .withHeader("custom_header2", "value2")
                .build();
        }

        @GET
        @Path("with-builder/headers/empty-body")
        public Mono<Void> methodReturningWithHeadersWithEmptyBodyWithBuilder() {
            return ResponseDecorator.of(Mono.<Void>empty())
                .withHeader("custom_header", "value")
                .build();
        }

        @GET
        @Path("with-builder/headers/deferred")
        public Mono<String> methodReturningDeferredHeadersWithBuilder() {
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
        public Mono<String> methodReturningStatusWithBuilder() {
            return ResponseDecorator.of(just("body"))
                .withStatus(HttpResponseStatus.MOVED_PERMANENTLY)
                .build();
        }

        @GET
        @Path("with-builder/status/empty")
        public Mono<Void> methodReturningStatusWithEmptyBodyWithBuilder() {
            return ResponseDecorator.of(Mono.<Void>empty())
                .withStatus(HttpResponseStatus.MOVED_PERMANENTLY)
                .build();
        }

        @GET
        @Path("with-builder/status/deferred")
        public Mono<String> methodReturningDeferredStatusWithBuilder() {
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
