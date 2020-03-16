package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;

import java.util.Set;

class JaxRsResourceCallContext implements JaxRsResourceInterceptor.JaxRsResourceContext {
    private final HttpServerRequest<ByteBuf> request;
    private final JaxRsResource<?> resource;

    public JaxRsResourceCallContext(HttpServerRequest<ByteBuf> request, JaxRsResource<?> resource) {
        this.request = request;
        this.resource = resource;
    }

    @Override
    public String getHttpMethod() {
        return resource.getHttpMethod().name();
    }

    @Override
    public String getRequestUri() {
        return request.getUri();
    }

    @Override
    public Set<String> getRequestHeaderNames() {
        return request.getHeaderNames();
    }

    @Override
    public String getRequestHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public String getResourcePath() {
        return resource.getPath();
    }
}
