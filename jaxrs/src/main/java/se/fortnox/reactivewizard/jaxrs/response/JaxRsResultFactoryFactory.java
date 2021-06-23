package se.fortnox.reactivewizard.jaxrs.response;

import se.fortnox.reactivewizard.jaxrs.JaxRsResource;
import se.fortnox.reactivewizard.jaxrs.Stream;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import se.fortnox.reactivewizard.util.FluxRxConverter;

import javax.inject.Inject;

public class JaxRsResultFactoryFactory {

    private final ResultTransformerFactories resultTransformerFactories;
    private final JsonSerializerFactory      jsonSerializerFactory;

    public JaxRsResultFactoryFactory() {
        this(new ResultTransformerFactories(new ResponseDecoratorTransformer(), new NoContentTransformer()), new JsonSerializerFactory());
    }

    @Inject
    public JaxRsResultFactoryFactory(ResultTransformerFactories resultTransformerFactories, JsonSerializerFactory jsonSerializerFactory) {
        this.resultTransformerFactories = resultTransformerFactories;
        this.jsonSerializerFactory = jsonSerializerFactory;
    }

    public <T> JaxRsResultFactory<T> createResultFactory(JaxRsResource<T> resource) {

        boolean isSingleType = FluxRxConverter.isSingleType(resource.getInstanceMethod().getReturnType());
        if (resource.getInstanceMethod().isAnnotationPresent(Stream.class) || !isSingleType) {
            return new JaxRsStreamingResultFactory<>(resource, resultTransformerFactories, jsonSerializerFactory);
        }

        return new JaxRsResultFactory<>(resource, resultTransformerFactories, jsonSerializerFactory);
    }
}
