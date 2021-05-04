package se.fortnox.reactivewizard.jaxrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.jaxrs.startupchecks.StartupCheck;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JaxRsResources {
    private static final Logger LOG = LoggerFactory.getLogger(JaxRsResources.class);
    private final Object[]      services;
    private       List<JaxRsResource>  resources;
    private       boolean              reloadClasses;
    private       JaxRsResourceFactory jaxRsResourceFactory;

    public JaxRsResources(Object[] services, JaxRsResourceFactory jaxRsResourceFactory, Boolean classReloading) {
        this(services, jaxRsResourceFactory, classReloading, Collections.emptySet());
    }

    public JaxRsResources(Object[] services, JaxRsResourceFactory jaxRsResourceFactory, Boolean classReloading, Set<StartupCheck> startupChecks) {
        this.services = services;
        this.reloadClasses = classReloading;
        this.jaxRsResourceFactory = jaxRsResourceFactory;

        this.resources = jaxRsResourceFactory.createResources(services);

        StringBuilder sb = new StringBuilder();
        for (JaxRsResource r : resources) {
            sb.append(System.lineSeparator());
            sb.append('\t');
            sb.append(r.toString());
        }
        LOG.info(sb.toString());

        startupChecks.forEach(startupCheck -> {
            startupCheck.check(resources);
        });
    }

    public JaxRsResource<?> findResource(JaxRsRequest request) {
        if (reloadClasses) {
            resources = jaxRsResourceFactory.createResources(services);
        }

        for (JaxRsResource<?> r : resources) {
            if (r.canHandleRequest(request)) {
                return r;
            }
        }
        return null;
    }
}
