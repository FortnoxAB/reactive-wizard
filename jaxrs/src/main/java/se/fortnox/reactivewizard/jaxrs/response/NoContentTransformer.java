package se.fortnox.reactivewizard.jaxrs.response;

import se.fortnox.reactivewizard.jaxrs.JaxRsResource;

/**
 * Kept only to avoid breaking changes. Logic moved to JaxRsResult.
 */
public class NoContentTransformer implements ResultTransformerFactory {
    @Override
    public <T> ResultTransformer<T> create(JaxRsResource<T> resource) {
        return (result, args) -> result;
    }
}
