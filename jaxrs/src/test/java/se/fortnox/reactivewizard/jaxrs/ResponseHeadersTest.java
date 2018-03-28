package se.fortnox.reactivewizard.jaxrs;

import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.response.ResponseHeaders;
import se.fortnox.reactivewizard.jaxrs.response.ResponseHeadersTransformer;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;
import se.fortnox.reactivewizard.jaxrs.response.ResultTransformerFactories;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;
import se.fortnox.reactivewizard.utils.JaxRsTestUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static rx.Observable.defer;
import static rx.Observable.just;

public class ResponseHeadersTest {

    @Test
    public void shouldReturnHeaderFromResource() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/");
        assertThat(response.getHeader("custom_header")).isEqualTo("value");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Test
    public void shouldReturnDeferedHeaderFromResource() {
        MockHttpServerResponse response = JaxRsTestUtil.get(new ResourceWithHeaders(), "/deferred");
        assertThat(response.getHeader("custom_header")).isEqualTo("deferred");
        assertThat(response.getOutp()).isEqualTo("\"body\"");
    }

    @Path("/")
    public class ResourceWithHeaders {
        @GET
        public Observable<String> methodReturningHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("custom_header", "value");
            return ResponseHeaders.withHeaders(just("body"), headers);
        }

        @GET
        @Path("deferred")
        public Observable<String> methodReturningDeferredHeaders() {
            Map<String, String> headers = new HashMap<>();
            return ResponseHeaders.withHeaders(defer(()->{
                headers.put("custom_header", "deferred");
                return just("body");
            }), headers);
        }
    }
}
