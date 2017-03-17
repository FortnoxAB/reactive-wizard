package se.fortnox.reactivewizard.jaxrs.response;

import se.fortnox.reactivewizard.jaxrs.JaxRsResource;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import rx.Observable;

public class JaxRsStreamingResultFactory<T> extends JaxRsResultFactory<T> {
    public JaxRsStreamingResultFactory(JaxRsResource<T> resource, ResultTransformerFactories resultTransformerFactories, JsonSerializerFactory jsonSerializerFactory) {
        super(resource, resultTransformerFactories, jsonSerializerFactory);
    }

    @Override
    public JaxRsResult<T> createResult(Observable<T> output, Object[] args) {
        return new JaxRsStreamingResult<>(output,
                responseStatus,
                serializer,
                headers
        );
    }
}
