package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.fortnox.reactivewizard.metrics.HealthRecorder;

import javax.inject.Inject;

/**
 * Handles creation of HttpClients from a given config, for easy creation of custom clients.
 */
public class HttpClientProvider {

    private final HealthRecorder              healthRecorder;
    private final ObjectMapper                objectMapper;
    private final RequestParameterSerializers requestParameterSerializers;

    @Inject
    public HttpClientProvider(HealthRecorder healthRecorder, ObjectMapper objectMapper, RequestParameterSerializers requestParameterSerializers) {
        this.healthRecorder = healthRecorder;
        this.objectMapper = objectMapper;
        this.requestParameterSerializers = requestParameterSerializers;
    }

    public HttpClient createClient(HttpClientConfig config) {
        return new HttpClient(config, new RxClientProvider(config, healthRecorder), objectMapper, requestParameterSerializers);
    }
}
