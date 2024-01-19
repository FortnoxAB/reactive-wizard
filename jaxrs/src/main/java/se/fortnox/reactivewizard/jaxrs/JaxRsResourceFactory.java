package se.fortnox.reactivewizard.jaxrs;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class JaxRsResourceFactory {

    protected final ParamResolverFactories    paramResolverFactories;
    protected final JaxRsResultFactoryFactory jaxRsResultFactoryFactory;
    private final RequestLogger requestLogger;

    public JaxRsResourceFactory() {
        this(new ParamResolverFactories(), new JaxRsResultFactoryFactory(), new RequestLogger());
    }

    @Inject
    public JaxRsResourceFactory(ParamResolverFactories paramResolverFactories,
                                JaxRsResultFactoryFactory jaxRsResultFactoryFactory,
                                RequestLogger requestLogger) {
        this.paramResolverFactories = paramResolverFactories;
        this.jaxRsResultFactoryFactory = jaxRsResultFactoryFactory;
        this.requestLogger = requestLogger;
    }

    /**
     * Create resources from services.
     * @param services the services
     * @return the resources
     */
    public List<JaxRsResource> createResources(Object[] services) {
        List<JaxRsResource> resources = new ArrayList<JaxRsResource>();
        for (Object service : services) {
            createResources(service, resources);
        }
        Collections.sort(resources);
        return resources;
    }

    /**
     * Create resource from service and to list of resources.
     * @param service the service
     * @param resources the other resources
     */
    public void createResources(Object service, List<JaxRsResource> resources) {
        Class<? extends Object> cls  = service.getClass();
        Path                    path = JaxRsMeta.getPath(cls);
        if (path == null) {
            throw new RuntimeException(
                "Service " + cls + " does not have @Path annotation");
        }
        for (Method m : cls.getMethods()) {
            if (m.getDeclaringClass().equals(Object.class)) {
                continue;
            }
            JaxRsResource jaxRsResource = createResource(path, m, service);
            if (jaxRsResource != null) {
                resources.add(jaxRsResource);
            }
        }

    }

    protected JaxRsResource createResource(Path clsPath, Method method, Object service) {
        JaxRsMeta meta = new JaxRsMeta(method, clsPath);

        if (meta.getHttpMethod() != null) {
            return createResource(method, service, meta);
        }
        return null;
    }

    protected JaxRsResource createResource(Method method, Object service, JaxRsMeta meta) {
        return new JaxRsResource(method, service, paramResolverFactories, jaxRsResultFactoryFactory, meta, requestLogger);
    }
}
