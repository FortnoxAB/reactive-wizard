package se.fortnox.reactivewizard.client;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.UseHttpClientConfig;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.ConfigClassScanner;
import se.fortnox.reactivewizard.binding.scanners.JaxRsClassScanner;
import se.fortnox.reactivewizard.config.Config;

import java.lang.reflect.Proxy;
import java.util.Map;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class RestClientFactory implements AutoBindModule {

    private static final Logger             log = LoggerFactory.getLogger(RestClientFactory.class);
    private final        JaxRsClassScanner  jaxRsClassScanner;
    private final        ConfigClassScanner configClassScanner;

    @Inject
    public RestClientFactory(JaxRsClassScanner jaxRsClassScanner, ConfigClassScanner configClassScanner) {
        this.jaxRsClassScanner = jaxRsClassScanner;
        this.configClassScanner = configClassScanner;
    }

    private <T> Provider<T> provider(Class<T> iface,
        Provider<HttpClientProvider> httpClientProvider, Provider<? extends HttpClientConfig> httpClientConfigProvider) {
        return () -> {

            HttpClientConfig httpClientConfig = httpClientConfigProvider.get();

            //Create client based on config and create proxy
            T httpProxy = httpClientProvider.get().createClient(httpClientConfig).create(iface);

            log.debug("Created {} for {} with custom httpClient: {}", Proxy.getInvocationHandler(httpProxy), iface.getName(), httpClientConfig);
            return httpProxy;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void configure(Binder binder) {
        Multibinder.newSetBinder(binder, PreRequestHook.class);
        Multibinder.newSetBinder(binder, RequestParameterSerializer.class);
        Provider<HttpClientProvider> httpClientProvider = binder.getProvider(HttpClientProvider.class);

        Map<String, Class<?>> configClassesByName = configClassScanner
            .getClasses()
            .stream()
            .collect(toMap(configClass -> configClass.getAnnotation(Config.class).value(), identity()));

        jaxRsClassScanner.getClasses().forEach(cls -> {

            Class httpClientConfigClass = getHttpClientConfigClass(configClassesByName, cls);

            Provider<? extends HttpClientConfig> httpClientConfigProvider = binder.getProvider(httpClientConfigClass);

            Provider<?> jaxRsClientProvider = provider(cls, httpClientProvider, httpClientConfigProvider);
            binder.bind((Class)cls)
                .toProvider(jaxRsClientProvider)
                .in(Scopes.SINGLETON);
        });
    }

    private Class getHttpClientConfigClass(Map<String, Class<?>> configClassesByName, Class<?> cls) {
        Class httpClientConfigClass = HttpClientConfig.class;

        if (cls.isAnnotationPresent(UseHttpClientConfig.class)) {
            String configName = cls.getAnnotation(UseHttpClientConfig.class).value();

            httpClientConfigClass = configClassesByName.get(configName);

            if (httpClientConfigClass == null) {
                throw new IllegalArgumentException(format("No config class found for config %s", configName));
            }

            if (!HttpClientConfig.class.isAssignableFrom(httpClientConfigClass)) {
                throw new IllegalArgumentException(format("Illegal argument in UseHttpClientConfig annotation. " +
                    "Configpath %s is of %s but must be subclass of HttpClientConfig", configName, httpClientConfigClass));
            }
        }
        return httpClientConfigClass;
    }

    @Override
    public Integer getPrio() {
        // Low prio to allow for local implementations to bind to the interface
        return 0;
    }
}
