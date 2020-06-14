package se.fortnox.reactivewizard.jaxrs.context;

import org.reactivestreams.Publisher;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceInterceptor;

public class JaxRsRequestContextInterceptor implements JaxRsResourceInterceptor {
    @Override
    public void preHandle(JaxRsResourceContext context) {
        JaxRsRequestContext.open();
    }

    @Override
    public void postHandle(JaxRsResourceContext context, Publisher<Void> resourceCall) {
        JaxRsRequestContext.close();
    }
}
