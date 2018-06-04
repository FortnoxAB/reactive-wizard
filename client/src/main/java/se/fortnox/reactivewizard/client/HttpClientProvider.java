package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.fortnox.reactivewizard.metrics.HealthRecorder;

import javax.inject.Inject;
import java.util.Set;

/**
 * Handles creation of HttpClients from a given config, for easy creation of custom clients.
 */
public class HttpClientProvider {

    private final HealthRecorder              healthRecorder;
    private final ObjectMapper                objectMapper;
    private final RequestParameterSerializers requestParameterSerializers;
    private final Set<PreRequestHook>         preRequestHooks;

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

    public HttpClient createClient(HttpClientConfig config) {
        return new HttpClient(config, new RxClientProvider(config, healthRecorder), objectMapper, requestParameterSerializers, preRequestHooks);
    }
}
