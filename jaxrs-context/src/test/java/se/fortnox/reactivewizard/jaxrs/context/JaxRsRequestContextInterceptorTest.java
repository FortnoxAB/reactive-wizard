package se.fortnox.reactivewizard.jaxrs.context;

import org.junit.After;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class JaxRsRequestContextInterceptorTest {
    private JaxRsRequestContextInterceptor interceptor = new JaxRsRequestContextInterceptor();

    @After
    public void tearDown() {
        JaxRsRequestContext.close();
    }

    @Test
    public void shouldOpenContextOnHandle() {
        interceptor.preHandle(null);
        assertThat(JaxRsRequestContext.getContext()).isNotNull();
    }

    @Test
    public void shouldCloseContextAfterHandle() {
        JaxRsRequestContext.open();

        interceptor.postHandle(null, null);
        assertThat(JaxRsRequestContext.getContext()).isNull();
    }
}
