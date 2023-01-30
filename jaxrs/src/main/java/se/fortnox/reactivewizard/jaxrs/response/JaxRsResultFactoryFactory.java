package se.fortnox.reactivewizard.jaxrs.response;

import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;
import se.fortnox.reactivewizard.jaxrs.Stream;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;

import javax.inject.Inject;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

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
        if (!isStreamAnnotationPresent(resource) && isFluxByteArray(resource)) {
            String className = resource.getResourceMethod().getDeclaringClass().getSimpleName();
            String methodName = resource.getResourceMethod().getName();
            throw new ResultFactoryException(String.format("Failed to create a result factory for non-streamable %s::%s." +
                " Annotate this method with @Stream to resolve the problem.", className, methodName));
        }

        if (isStreamAnnotationPresent(resource)) {
            return new JaxRsStreamingResultFactory<>(resource, resultTransformerFactories, jaxRsResultSerializerFactory);
        }

        return new JaxRsResultFactory<>(resource, resultTransformerFactories, jaxRsResultSerializerFactory);
    }

    private <T> boolean isStreamAnnotationPresent(JaxRsResource<T> resource) {
        return resource.getInstanceMethod().isAnnotationPresent(Stream.class);
    }

    private <T> boolean isFluxByteArray(JaxRsResource<T> resource) {
        return resource.getInstanceMethod().getReturnType().isAssignableFrom(Flux.class)
            && resource.getProduces().equals(APPLICATION_OCTET_STREAM);
    }

    public static final class ResultFactoryException extends RuntimeException {
        private ResultFactoryException() {
        }

        ResultFactoryException(String message) {
            super(message);
        }
    }
}
