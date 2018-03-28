package se.fortnox.reactivewizard.jaxrs.response;

import se.fortnox.reactivewizard.jaxrs.JaxRsResource;

/**
 * Adds headers returned from a resource.
 */
public class ResponseDecoratorTransformer implements ResultTransformerFactory {
    @Override
    public <T> ResultTransformer<T> create(JaxRsResource<T> resource) {
        return (result, args) -> result.map(output -> {
            return ResponseDecorator.apply(output, result);
        });
    }

    @Override
    public Integer getPrio() {
        // Because this transformer depends on the type of Observable returned, it needs to run first
        return 0;
    }
}
