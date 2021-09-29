package se.fortnox.reactivewizard.client;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.HttpConfigClassScanner;
import se.fortnox.reactivewizard.binding.scanners.JaxRsClassScanner;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class RestClientFactory implements AutoBindModule {

    private static final Logger                 LOG = LoggerFactory.getLogger(RestClientFactory.class);
    private final        JaxRsClassScanner      jaxRsClassScanner;
    private final        HttpConfigClassScanner httpConfigClassScanner;

    @Inject
    public RestClientFactory(JaxRsClassScanner jaxRsClassScanner, HttpConfigClassScanner httpConfigClassScanner) {
        this.jaxRsClassScanner = jaxRsClassScanner;
        this.httpConfigClassScanner = httpConfigClassScanner;
    }

    private <T> Provider<T> provider(Class<T> iface,
        Provider<HttpClientProvider> httpClientProvider, Provider<? extends HttpClientConfig> httpClientConfigProvider) {
        return () -> {

            HttpClientConfig httpClientConfig = httpClientConfigProvider.get();

            //Create client based on config and create proxy
            T httpProxy = httpClientProvider.get().createClient(httpClientConfig).create(iface);

            LOG.debug("Created {} for {} with custom httpClient: {}", Proxy.getInvocationHandler(httpProxy), iface.getName(), httpClientConfig);
            return httpProxy;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void configure(Binder binder) {
        Multibinder.newSetBinder(binder, PreRequestHook.class);
        Multibinder.newSetBinder(binder, RequestParameterSerializer.class);
        Provider<HttpClientProvider> httpClientProvider = binder.getProvider(HttpClientProvider.class);

        Map<Class, Class<?>> httpClientConfigByResource = new HashMap<>();

        httpConfigClassScanner
            .getClasses()
            .forEach(configClass -> {
                if (configClass.isAnnotationPresent(UseInResource.class)) {
                    for (Class resource : configClass.getAnnotation(UseInResource.class).value()) {
                        if (!resource.isInterface()) {
                            throw new IllegalArgumentException(format(
                                "class %s pointed out in UseInResource annotation must be an interface",
                                resource.getCanonicalName()));
                        }
                        httpClientConfigByResource.put(resource, configClass);
                    }
                }
            });

        jaxRsClassScanner.getClasses().forEach(cls -> {

            Class httpClientConfigClass = httpClientConfigByResource.getOrDefault(cls, HttpClientConfig.class);

            Provider<? extends HttpClientConfig> httpClientConfigProvider = binder.getProvider(httpClientConfigClass);

            Provider<?> jaxRsClientProvider = provider(cls, httpClientProvider, httpClientConfigProvider);
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
