package se.fortnox.reactivewizard.reactorclient;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.HttpConfigClassScanner;
import se.fortnox.reactivewizard.binding.scanners.JaxRsClassScanner;
import se.fortnox.reactivewizard.client.HttpClientConfig;
import se.fortnox.reactivewizard.client.PreRequestHook;
import se.fortnox.reactivewizard.client.RequestParameterSerializer;
import se.fortnox.reactivewizard.client.RestClientFactory;
import se.fortnox.reactivewizard.client.UseInResource;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class ReactorRestClientFactory implements AutoBindModule {

    private final JaxRsClassScanner      jaxRsClassScanner;
    private final HttpConfigClassScanner httpConfigClassScanner;

    @Inject
    public ReactorRestClientFactory(JaxRsClassScanner jaxRsClassScanner, HttpConfigClassScanner httpConfigClassScanner) {
        this.jaxRsClassScanner = jaxRsClassScanner;
        this.httpConfigClassScanner = httpConfigClassScanner;

        //Adding all the micrometer metrics to the default registry
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM);
        ((CompositeMeterRegistry)reactor.netty.Metrics.REGISTRY).add(registry);
    }

    private <T> Provider<T> provider(Class<T> iface,
        Provider<ReactorHttpClientProvider> httpClientProvider, Provider<? extends HttpClientConfig> httpClientConfigProvider) {
        return () -> {

            HttpClientConfig httpClientConfig = httpClientConfigProvider.get();

            //Create client based on config and create proxy
            T httpProxy = httpClientProvider.get().createClient(httpClientConfig).create(iface);

            return httpProxy;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void configure(Binder binder) {
        Multibinder.newSetBinder(binder, PreRequestHook.class);
        Multibinder.newSetBinder(binder, RequestParameterSerializer.class);
        Provider<ReactorHttpClientProvider> httpClientProvider = binder.getProvider(ReactorHttpClientProvider.class);

        Map<Class, Class<?>> httpClientConfigByResource = new HashMap<>();

        httpConfigClassScanner
            .getClasses()
            .forEach(configClass -> {
                if (configClass.isAnnotationPresent(UseInResource.class)) {
                    for (Class resource : configClass.getAnnotation(UseInResource.class).value()) {
                        if (!resource.isInterface()) {
                            throw new IllegalArgumentException(format("%s pointed out in UseInResource annotation must be an interface", resource));
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
        return new RestClientFactory(null, null).getPrio() + 1;
    }
}
