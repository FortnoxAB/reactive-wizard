package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;

/**
 * Sets the response status to 204 NO CONTENT if the returned Observable is empty
 */
public class NoContentTransformer implements ResultTransformerFactory {
    @Override
    public <T> ResultTransformer<T> create(JaxRsResource<T> resource) {
        return (result, args) -> result;
    }
}
