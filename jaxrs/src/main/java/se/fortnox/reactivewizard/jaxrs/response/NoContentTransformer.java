package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;

/**
 * Sets the response status to 204 NO CONTENT for successful responses if the returned Observable is empty
 */
public class NoContentTransformer implements ResultTransformerFactory {
    @Override
    public <T> ResultTransformer<T> create(JaxRsResource<T> resource) {
        return (result, args) -> result.map(output -> {
            return output.switchIfEmpty(Flux.defer(() -> {
                if (result.responseStatus.codeClass() == HttpStatusClass.SUCCESS) {
                    result.responseStatus = HttpResponseStatus.NO_CONTENT;
                }

                return Flux.empty();
            }));
        });
    }
}
