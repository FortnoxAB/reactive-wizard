package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles creation of HttpClients from a given config, for easy creation of custom clients.
 */
@Singleton
public class HttpClientProvider {
    protected final ObjectMapper                                             objectMapper;
    protected final RequestParameterSerializers                              requestParameterSerializers;
    protected final Set<PreRequestHook>                                             preRequestHooks;
    private final HealthRecorder healthRecorder;
    private final   Map<Class<? extends HttpClientConfig>, ReactorRxClientProvider> rxClientProviderCache = new ConcurrentHashMap<>();
    private final RequestLogger requestLogger;

    @Inject
    public HttpClientProvider(ObjectMapper objectMapper,
                              RequestParameterSerializers requestParameterSerializers,
                              Set<PreRequestHook> preRequestHooks,
                              HealthRecorder healthRecorder,
                              RequestLogger requestLogger) {
        this.objectMapper = objectMapper;
        this.requestParameterSerializers = requestParameterSerializers;
        this.preRequestHooks = preRequestHooks;
        this.healthRecorder = healthRecorder;
        this.requestLogger = requestLogger;
    }

    private ReactorRxClientProvider getRxClientProvider(HttpClientConfig httpClientConfig) {
        return rxClientProviderCache.computeIfAbsent(httpClientConfig.getClass(),
            config -> new ReactorRxClientProvider(httpClientConfig, healthRecorder));
    }

    protected HttpClient instantiateClient(HttpClientConfig httpClientConfig, ReactorRxClientProvider rxClientProvider) {
        return new HttpClient(httpClientConfig, rxClientProvider, objectMapper, requestParameterSerializers, preRequestHooks, requestLogger);
    }

    public HttpClient createClient(HttpClientConfig httpClientConfig) {
        return instantiateClient(httpClientConfig, getRxClientProvider(httpClientConfig));
    }
}
