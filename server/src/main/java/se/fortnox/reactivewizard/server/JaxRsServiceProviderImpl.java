package se.fortnox.reactivewizard.server;

import se.fortnox.reactivewizard.jaxrs.JaxRsServiceProvider;

import javax.inject.Provider;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JaxRsServiceProviderImpl implements JaxRsServiceProvider {

	private final List<Provider<?>> resourceProviders = new ArrayList<>();
	private Object [] services = null;

	public Object[] getServices() {
		if (services == null) {
			services = resourceProviders.stream().map(Provider::get).toArray();
		}
		return services;
	}

    /**
     * Takes an implement
     * @param cls
     * @return
     */
	public static Optional<Class<?>> getJaxRsClass(Class<?> cls) {
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

	public void addJaxRsResourceProvider(Provider<?> provider) {
		resourceProviders.add(provider);
	}


}
