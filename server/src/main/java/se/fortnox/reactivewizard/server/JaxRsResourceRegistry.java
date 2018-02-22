package se.fortnox.reactivewizard.server;

import se.fortnox.reactivewizard.jaxrs.JaxRsResourcesProvider;

import javax.inject.Provider;
import javax.ws.rs.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Keeps track of what JAX-RS resources are available.
 */
public class JaxRsResourceRegistry implements JaxRsResourcesProvider {
    private List<Provider<?>> resources = new LinkedList<>();

    public Optional<Class<?>> getJaxRsClass(Class<?> cls) {
        if (!cls.isInterface()) {
            if (cls.getAnnotation(Path.class) != null) {
                return Optional.of(cls);
            }

            for (Class<?> iface : cls.getInterfaces()) {
                if (iface.getAnnotation(Path.class) != null) {
                    return Optional.of(iface);
                }
            }
        }
        return Optional.empty();
    }

    public void add(Provider<?> jaxRsClass) {
        resources.add(jaxRsClass);
    }

    @Override
    public Object[] getResources() {
        return resources.stream().map(Provider::get).toArray();
    }
}
