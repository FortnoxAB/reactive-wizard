package se.fortnox.reactivewizard.jaxrs;

import reactor.netty.http.server.HttpServerRequest;

import java.util.Set;

class JaxRsResourceCallContext implements JaxRsResourceInterceptor.JaxRsResourceContext {
    private final HttpServerRequest request;
    private final JaxRsResource<?> resource;

    public JaxRsResourceCallContext(HttpServerRequest request, JaxRsResource<?> resource) {
        this.request = request;
        this.resource = resource;
    }

    @Override
    public String getHttpMethod() {
        return resource.getHttpMethod().name();
    }

    @Override
    public String getRequestUri() {
        return request.uri();
    }

    @Override
    public Set<String> getRequestHeaderNames() {
        return request.requestHeaders().names();
    }

    @Override
    public String getRequestHeader(String name) {
        return request.requestHeaders().get(name);
    }

    @Override
    public String getResourcePath() {
        return resource.getPath();
    }
}
