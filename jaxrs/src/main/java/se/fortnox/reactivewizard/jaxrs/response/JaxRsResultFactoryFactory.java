package se.fortnox.reactivewizard.jaxrs.response;

import jakarta.inject.Inject;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;
import se.fortnox.reactivewizard.jaxrs.Stream;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;

public class JaxRsResultFactoryFactory {

    private final ResultTransformerFactories resultTransformerFactories;
    private final JaxRsResultSerializerFactory jaxRsResultSerializerFactory;

    public JaxRsResultFactoryFactory() {
        this(new ResultTransformerFactories(new ResponseDecoratorTransformer(), new NoContentTransformer()),
            new JaxRsResultSerializerFactory(new JsonSerializerFactory()));
    }

    @Inject
    public JaxRsResultFactoryFactory(ResultTransformerFactories resultTransformerFactories, JaxRsResultSerializerFactory jaxRsResultSerializerFactory) {
        this.resultTransformerFactories = resultTransformerFactories;
        this.jaxRsResultSerializerFactory = jaxRsResultSerializerFactory;
    }

    /**
     * Create a result factory.
     * @param resource the resource
     * @param <T> the type of resource
     * @return the result factory
     */
    public <T> JaxRsResultFactory<T> createResultFactory(JaxRsResource<T> resource) {
        if (isStreamAnnotationPresent(resource)) {
            return new JaxRsStreamingResultFactory<>(resource, resultTransformerFactories, jaxRsResultSerializerFactory);
        }

        return new JaxRsResultFactory<>(resource, resultTransformerFactories, jaxRsResultSerializerFactory);
    }

    private <T> boolean isStreamAnnotationPresent(JaxRsResource<T> resource) {
        return resource.getInstanceMethod().isAnnotationPresent(Stream.class);
    }

}
