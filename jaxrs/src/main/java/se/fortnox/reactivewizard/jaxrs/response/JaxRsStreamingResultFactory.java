package se.fortnox.reactivewizard.jaxrs.response;

import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;
import se.fortnox.reactivewizard.jaxrs.Stream;
import se.fortnox.reactivewizard.util.FluxRxConverter;

public class JaxRsStreamingResultFactory<T> extends JaxRsResultFactory<T> {
    public JaxRsStreamingResultFactory(JaxRsResource<T> resource,
                                       ResultTransformerFactories resultTransformerFactories,
                                       JaxRsResultSerializerFactory jaxRsResultSerializerFactory) {
        super(resource, resultTransformerFactories, jaxRsResultSerializerFactory);

        Stream.Type streamType = resource.getInstanceMethod().getAnnotation(Stream.class).value();
        boolean isFlux = FluxRxConverter.isFlux(resource.getInstanceMethod().getReturnType());

        serializer = jaxRsResultSerializerFactory.createStreamingSerializer(
            resource.getProduces(),
            rawReturnType,
            streamType,
            isFlux);
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
