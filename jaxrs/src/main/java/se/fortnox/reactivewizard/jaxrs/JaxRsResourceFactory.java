package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class JaxRsResourceFactory {

    protected final ParamResolverFactories paramResolverFactories;
    protected final JaxRsResultFactoryFactory jaxRsResultFactoryFactory;
    protected final BlockingResourceScheduler blockingResourceScheduler;

    public JaxRsResourceFactory() {
        this(new ParamResolverFactories(), new JaxRsResultFactoryFactory(), new BlockingResourceScheduler());
    }

    @Inject
    public JaxRsResourceFactory(ParamResolverFactories paramResolverFactories, JaxRsResultFactoryFactory jaxRsResultFactoryFactory, BlockingResourceScheduler blockingResourceScheduler) {
        this.paramResolverFactories = paramResolverFactories;
        this.jaxRsResultFactoryFactory = jaxRsResultFactoryFactory;
        this.blockingResourceScheduler = blockingResourceScheduler;
    }

    public List<JaxRsResource> createResources(Object[] services) {
        List<JaxRsResource> resources = new ArrayList<JaxRsResource>();
        for (Object service : services) {
            createResources(service, resources);
        }
        Collections.sort(resources);
        return resources;
    }

    public void createResources(Object service,
                                       List<JaxRsResource> resources) {

        Class<? extends Object> cls = service.getClass();
        Path path = JaxRsMeta.getPath(cls);
        if (path == null) {
            throw new RuntimeException(
                    "Service " + cls + " does not have @Path annotation");
        }
        for (Method m : cls.getMethods()) {
            if (m.getDeclaringClass().equals(Object.class)) {
                continue;
            }
            JaxRsResource r = createResource(path, m, service);
            if (r != null) {
                resources.add(r);
            }
        }

    }

    protected JaxRsResource createResource(Path clsPath, Method m, Object service) {

        JaxRsMeta meta = new JaxRsMeta(m, clsPath);

        if (meta.getHttpMethod() != null) {
            return createResource(m, service, meta);
        }
        return null;
    }

    protected JaxRsResource createResource(Method m, Object service, JaxRsMeta meta) {
        return new JaxRsResource(m,
                service,
                paramResolverFactories,
                jaxRsResultFactoryFactory,
                blockingResourceScheduler,
                meta);
    }
}
