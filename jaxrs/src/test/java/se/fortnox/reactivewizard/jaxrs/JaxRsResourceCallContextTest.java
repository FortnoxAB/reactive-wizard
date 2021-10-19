package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.netty.http.server.HttpServerRequest;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JaxRsResourceCallContextTest {
    @Mock
    private HttpServerRequest request;

    @Mock
    private JaxRsResource<?> resource;

    @Before
    public void setUp() {
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.add("foo", "bar");
        when(request.requestHeaders()).thenReturn(headers);
        when(request.uri()).thenReturn("/foo?bar=true");

        when(resource.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(resource.getPath()).thenReturn("/foo");
    }

    @Test
    public void verifyValues() {
        JaxRsResourceCallContext callContext = new JaxRsResourceCallContext(request, resource);
        assertThat(callContext.getHttpMethod()).isEqualTo("GET");
        assertThat(callContext.getRequestHeader("foo")).isEqualTo("bar");
        assertThat(callContext.getRequestHeaderNames().iterator().next()).isEqualTo("foo");
        assertThat(callContext.getRequestUri()).isEqualTo("/foo?bar=true");
        assertThat(callContext.getResourcePath()).isEqualTo("/foo");
    }
}
