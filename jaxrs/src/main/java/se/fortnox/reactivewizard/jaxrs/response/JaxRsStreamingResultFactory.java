package se.fortnox.reactivewizard.jaxrs.response;

import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;

public class JaxRsStreamingResultFactory<T> extends JaxRsResultFactory<T> {
    public JaxRsStreamingResultFactory(JaxRsResource<T> resource,
        ResultTransformerFactories resultTransformerFactories,
        JsonSerializerFactory jsonSerializerFactory
    ) {
        super(resource, resultTransformerFactories, jsonSerializerFactory);
    }

    @Override
    public JaxRsResult<T> createResult(Flux<T> output, Object[] args) {
        return new JaxRsStreamingResult<>(output,
            responseStatus,
            serializer,
            headers
        );
    }
}
