package se.fortnox.reactivewizard.reactorclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.fortnox.reactivewizard.client.HttpClientConfig;
import se.fortnox.reactivewizard.client.PreRequestHook;
import se.fortnox.reactivewizard.client.RequestParameterSerializers;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReactorHttpClientProvider {
    protected final ObjectMapper                                             objectMapper;
    protected final RequestParameterSerializers                              requestParameterSerializers;
    protected final Set<PreRequestHook>                                             preRequestHooks;
    private final   Map<Class<? extends HttpClientConfig>, ReactorRxClientProvider> rxClientProviderCache = new HashMap<>();

    @Inject
    public ReactorHttpClientProvider(ObjectMapper objectMapper,
        RequestParameterSerializers requestParameterSerializers,
        Set<PreRequestHook> preRequestHooks
    ) {
        this.objectMapper = objectMapper;
        this.requestParameterSerializers = requestParameterSerializers;
        this.preRequestHooks = preRequestHooks;
    }

    private ReactorRxClientProvider getRxClientProvider(HttpClientConfig httpClientConfig) {
        return rxClientProviderCache.computeIfAbsent(httpClientConfig.getClass(),
            config -> new ReactorRxClientProvider(httpClientConfig));
    }

    protected ReactorHttpClient instantiateClient(HttpClientConfig httpClientConfig, ReactorRxClientProvider rxClientProvider) {
        return new ReactorHttpClient(httpClientConfig, rxClientProvider, objectMapper, requestParameterSerializers, preRequestHooks);
    }

    public ReactorHttpClient createClient(HttpClientConfig httpClientConfig) {
        return instantiateClient(httpClientConfig, getRxClientProvider(httpClientConfig));
    }
}
