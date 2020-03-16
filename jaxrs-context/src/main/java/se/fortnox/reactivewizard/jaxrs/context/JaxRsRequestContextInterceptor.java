package se.fortnox.reactivewizard.jaxrs.context;

import rx.Observable;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceInterceptor;

public class JaxRsRequestContextInterceptor implements JaxRsResourceInterceptor {
    @Override
    public void preHandle(JaxRsResourceContext context) {
        JaxRsRequestContext.open();
    }

    @Override
    public void postHandle(JaxRsResourceContext context, Observable<Void> resourceCall) {
        JaxRsRequestContext.close();
    }
}
