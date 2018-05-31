package se.fortnox.reactivewizard.client;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.JaxRsClassScanner;

import java.lang.reflect.Proxy;

public class RestClientFactory implements AutoBindModule {

    private static final Logger            log = LoggerFactory.getLogger(RestClientFactory.class);
    private final        JaxRsClassScanner jaxRsClassScanner;

    @Inject
    public RestClientFactory(JaxRsClassScanner jaxRsClassScanner) {
        this.jaxRsClassScanner = jaxRsClassScanner;
    }

    private <T> Provider<T> provider(Class<T> iface, Provider<HttpClient> hcp) {
        return new Provider<T>() {
            @Override
            public T get() {
                T httpProxy = hcp.get().create(iface);
                log.debug("Created " + Proxy.getInvocationHandler(httpProxy)
                    + " for " + iface.getName());
                return httpProxy;
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void configure(Binder binder) {
        Multibinder.newSetBinder(binder, TypeLiteral.get(RequestParameterSerializer.class));
        Provider<HttpClient> httpClientProvider = binder.getProvider(HttpClient.class);
        jaxRsClassScanner.getClasses().forEach(cls -> {
            Provider<?> jaxRsClientProvider = provider(cls, httpClientProvider);
            binder.bind((Class)cls)
                .toProvider(jaxRsClientProvider)
                .in(Scopes.SINGLETON);
        });
    }

    @Override
    public Integer getPrio() {
        // Low prio to allow for local implementations to bind to the interface
        return 0;
    }
}
