package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.fortnox.reactivewizard.metrics.HealthRecorder;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles creation of HttpClients from a given config, for easy creation of custom clients.
 */
public class HttpClientProvider {

    private final HealthRecorder                          healthRecorder;
    private final ObjectMapper                            objectMapper;
    private final RequestParameterSerializers             requestParameterSerializers;
    private final Set<PreRequestHook>                     preRequestHooks;
    private final Map<HttpClientConfig, RxClientProvider> rxClientProviderCache = new HashMap<>();

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

        //Fill up cache with RxClientProviders
        rxClientProviderCache.computeIfAbsent(httpClientConfig, config -> new RxClientProvider(config, healthRecorder));

        //Create client
        return new HttpClient(httpClientConfig, rxClientProviderCache.get(httpClientConfig), objectMapper, requestParameterSerializers, preRequestHooks);
    }
}
