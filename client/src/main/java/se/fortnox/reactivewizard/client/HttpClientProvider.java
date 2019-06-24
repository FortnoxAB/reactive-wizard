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

    private final HealthRecorder                          healthRecorder;
    private final ObjectMapper                            objectMapper;
    private final RequestParameterSerializers             requestParameterSerializers;
    private final Set<PreRequestHook>                     preRequestHooks;
    private final Map<Class<? extends HttpClientConfig>, RxClientProvider> rxClientProviderCache = new HashMap<>();

    @Inject
    public HttpClientProvider(HealthRecorder healthRecorder,
        ObjectMapper objectMapper,
        RequestParameterSerializers requestParameterSerializers,
        Set<PreRequestHook> preRequestHooks
    ) {
        this.healthRecorder = healthRecorder;
        this.objectMapper = objectMapper;
        this.requestParameterSerializers = requestParameterSerializers;
        this.preRequestHooks = preRequestHooks;
    }

    public HttpClient createClient(HttpClientConfig httpClientConfig) {
        return instantiateClient(httpClientConfig, getRxClientProvider(httpClientConfig), objectMapper, requestParameterSerializers, preRequestHooks);
    }

    protected HttpClient instantiateClient(HttpClientConfig httpClientConfig, RxClientProvider rxClientProvider,
        ObjectMapper objectMapper, RequestParameterSerializers requestParameterSerializers, Set<PreRequestHook> preRequestHooks) {
        //Create client
        return new HttpClient(httpClientConfig, rxClientProvider, objectMapper, requestParameterSerializers, preRequestHooks);
    }

    private RxClientProvider getRxClientProvider(HttpClientConfig httpClientConfig) {
        return rxClientProviderCache.computeIfAbsent(httpClientConfig.getClass(), config -> new RxClientProvider(httpClientConfig, healthRecorder));
    }
}
