package se.fortnox.reactivewizard.jaxrs;

/**
 * Provides JaxRs resources to the JaxRsRequestHandler.
 */
public interface JaxRsResourcesProvider {
    Object[] getResources();
}
