package se.fortnox.reactivewizard.jaxrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JaxRsResources {
    private static final Logger LOG = LoggerFactory.getLogger(JaxRsResources.class);
    private final Object[]      services;
    private       List<JaxRsResource>  resources;
    private       boolean              reloadClasses;
    private       JaxRsResourceFactory jaxRsResourceFactory;

    public JaxRsResources(Object[] services, JaxRsResourceFactory jaxRsResourceFactory, Boolean classReloading) {
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

    List<JaxRsResource> getResources() {
        return resources;
    }
}
