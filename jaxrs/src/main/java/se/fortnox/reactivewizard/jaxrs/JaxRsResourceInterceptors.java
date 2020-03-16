package se.fortnox.reactivewizard.jaxrs;

import rx.Observable;

import javax.inject.Inject;
import java.util.Set;

/**
 * Composition of all registered JaxRsResourceInterceptor instances.
 */
class JaxRsResourceInterceptors {
    private final Set<JaxRsResourceInterceptor> interceptors;

    @Inject
    public JaxRsResourceInterceptors(Set<JaxRsResourceInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    void preHandle(JaxRsResourceInterceptor.JaxRsResourceContext context) {
        interceptors.forEach(interceptor -> interceptor.preHandle(context));
    }

    void postHandle(JaxRsResourceInterceptor.JaxRsResourceContext context, Observable<Void> resourceCall) {
        interceptors.forEach(interceptor -> interceptor.postHandle(context, resourceCall));
    }
}
