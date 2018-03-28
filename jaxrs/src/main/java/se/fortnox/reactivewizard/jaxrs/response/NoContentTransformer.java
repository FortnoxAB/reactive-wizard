package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;

import static se.fortnox.reactivewizard.util.rx.RxUtils.doIfEmpty;

/**
 * Sets the response status to 204 NO CONTENT if the returned Observable is empty
 */
public class NoContentTransformer implements ResultTransformerFactory {
    @Override
    public <T> ResultTransformer<T> create(JaxRsResource<T> resource) {
        return (result, args) -> result.map(output -> {
            return doIfEmpty(output, () -> result.responseStatus = HttpResponseStatus.NO_CONTENT);
        });
    }
}
