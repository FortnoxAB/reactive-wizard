package se.fortnox.reactivewizard.jaxrs.startupchecks;

import se.fortnox.reactivewizard.jaxrs.JaxRsResource;

import java.util.List;

public interface StartupCheck {

    /**
     * This method may be used to perform startup checks.
     * If you want to prevent startup if the check fails you can throw an exception.
     * @param resources list of JaxRsResources
     */
    void check(List<JaxRsResource> resources);
}
