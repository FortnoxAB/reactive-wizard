package se.fortnox.reactivewizard.client;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.Before;
import org.junit.Test;
import se.fortnox.reactivewizard.util.ManifestUtil;

import java.net.InetSocketAddress;

import static org.fest.assertions.Assertions.assertThat;

public class RequestTracerTest {
    private static final String HOST_NAME                 = "localhost";
    private static final String VERSION                   = "0.0.1-SNAPSHOT";
    private static final String ARTIFACT_ID               = "reactivewizard-client";
    private static final String X_VIA_CLIENT_HEADER_VALUE = String.format("%s:%s:%s", HOST_NAME, ARTIFACT_ID, VERSION);

    private RequestTracer  requestTracer;
    private RequestBuilder requestBuilder;

    private static String getXViaClientValue(RequestBuilder requestBuilder) {
        return requestBuilder.getHeaders().get("X-Via-Client");
    }

    @Before
    public void setup() {
        // Fixture
        ManifestUtil.ManifestValues manifestValues = new ManifestUtil.ManifestValues(VERSION, ARTIFACT_ID);
        requestTracer = new RequestTracer(manifestValues, HOST_NAME);
        requestBuilder = new RequestBuilder(new InetSocketAddress("localhost", 8080), HttpMethod.GET, "key");
    }

    @Test
    public void shouldAddTraceToFirstRequestInChain() {
        // Execution
        requestTracer.addTrace(requestBuilder);

        // Verification
        assertThat(getXViaClientValue(requestBuilder)).isEqualTo(X_VIA_CLIENT_HEADER_VALUE);

    }

    @Test
    public void shouldAddTraceToSecondRequestInChain() {
        // Fixture
        requestBuilder.getHeaders().put("X-Via-Client", X_VIA_CLIENT_HEADER_VALUE);

        // Execution
        requestTracer.addTrace(requestBuilder);

        // Verification
        String xViaClientForRequestChainExpectedValue = String.format(
            "%s,%s", X_VIA_CLIENT_HEADER_VALUE, X_VIA_CLIENT_HEADER_VALUE
        );
        assertThat(getXViaClientValue(requestBuilder)).isEqualTo(xViaClientForRequestChainExpectedValue);
    }
}
