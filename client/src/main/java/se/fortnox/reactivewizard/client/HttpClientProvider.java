package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.fortnox.reactivewizard.metrics.HealthRecorder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles creation of HttpClients from a given config, for easy creation of custom clients.
 */
@Singleton
public class HttpClientProvider {
    protected final ObjectMapper                                             objectMapper;
    protected final RequestParameterSerializers                              requestParameterSerializers;
    protected final Set<PreRequestHook>                                             preRequestHooks;
    private final HealthRecorder healthRecorder;
    private final   Map<Class<? extends HttpClientConfig>, ReactorRxClientProvider> rxClientProviderCache = new HashMap<>();

    @Inject
    public HttpClientProvider(ObjectMapper objectMapper,
                                     RequestParameterSerializers requestParameterSerializers,
                                     Set<PreRequestHook> preRequestHooks,
                                     HealthRecorder healthRecorder
    ) {
        this.objectMapper = objectMapper;
        this.requestParameterSerializers = requestParameterSerializers;
        this.preRequestHooks = preRequestHooks;
        this.healthRecorder = healthRecorder;
    }

    private ReactorRxClientProvider getRxClientProvider(HttpClientConfig httpClientConfig) {
        return rxClientProviderCache.computeIfAbsent(httpClientConfig.getClass(),
            config -> new ReactorRxClientProvider(httpClientConfig, healthRecorder));
    }

    protected HttpClient instantiateClient(HttpClientConfig httpClientConfig, ReactorRxClientProvider rxClientProvider) {
        return new HttpClient(httpClientConfig, rxClientProvider, objectMapper, requestParameterSerializers, preRequestHooks);
    }

    public HttpClient createClient(HttpClientConfig httpClientConfig) {
        return instantiateClient(httpClientConfig, getRxClientProvider(httpClientConfig));
    }
}
