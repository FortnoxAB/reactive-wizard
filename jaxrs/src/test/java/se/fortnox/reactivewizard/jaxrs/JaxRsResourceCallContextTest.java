package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Collections.singleton;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JaxRsResourceCallContextTest {
    @Mock
    private HttpServerRequest<ByteBuf> request;

    @Mock
    private JaxRsResource<?> resource;

    @Before
    public void setUp() {
        when(request.getHeaderNames()).thenReturn(singleton("foo"));
        when(request.getHeader("foo")).thenReturn("bar");
        when(request.getUri()).thenReturn("/foo?bar=true");

        when(resource.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(resource.getPath()).thenReturn("/foo");
    }

    @Test
    public void verifyValues() {
        JaxRsResourceCallContext callContext = new JaxRsResourceCallContext(request, resource);
        assertThat(callContext.getHttpMethod()).isEqualTo("GET");
        assertThat(callContext.getRequestHeader("foo")).isEqualTo("bar");
        assertThat(callContext.getRequestHeaderNames()).isEqualTo(singleton("foo"));
        assertThat(callContext.getRequestUri()).isEqualTo("/foo?bar=true");
        assertThat(callContext.getResourcePath()).isEqualTo("/foo");
    }
}
