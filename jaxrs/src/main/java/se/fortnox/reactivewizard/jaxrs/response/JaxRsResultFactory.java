package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.jaxrs.Headers;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;
import se.fortnox.reactivewizard.jaxrs.SuccessStatus;
import se.fortnox.reactivewizard.util.FluxRxConverter;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class JaxRsResultFactory<T> {

    protected final HttpResponseStatus responseStatus;
    protected final Class<T>           rawReturnType;
    protected Function<Flux<T>, Flux<byte[]>> serializer;
    protected final Map<String, String> headers = new HashMap<>();
    private final ResultTransformer<T> transformers;

    public JaxRsResultFactory(JaxRsResource<T> resource, ResultTransformerFactories resultTransformerFactories,
                              JaxRsResultSerializerFactory jaxRsResultSerializerFactory) {
        Method method = resource.getResourceMethod();
        responseStatus = getSuccessStatus(resource);
        rawReturnType = getRawReturnType(method);

        boolean isFlux = FluxRxConverter.isFlux(method.getReturnType());
        serializer = jaxRsResultSerializerFactory.createSerializer(resource.getProduces(), rawReturnType, isFlux);

        transformers = resultTransformerFactories.createTransformers(resource);

        headers.put("Content-Type", resource.getProduces());

        Headers headerAnnotation = resource.getInstanceMethod().getAnnotation(Headers.class);

        if (headerAnnotation != null && headerAnnotation.value().length > 0) {
            for (String headerString : headerAnnotation.value()) {
                if (headerString.contains(":")) {
                    final String[] parts = headerString.split(":");
                    headers.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
    }


    /**
     * Create result.
     * @param output the output
     * @param args the arguments
     * @return the result
     */
    public JaxRsResult<T> createResult(Flux<T> output, Object[] args) {
        return new JaxRsResult<>(output,
            responseStatus,
            serializer,
            headers
        );
    }

    /**
     * Create result.
     * @param output the output
     * @param args the arguments
     * @return the result
     */
    public JaxRsResult<T> create(Flux<T> output, Object[] args) {
        JaxRsResult<T> result = createResult(output, args);
        result = transformers.apply(result, args);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Class<T> getRawReturnType(Method method) {
        if (FluxRxConverter.isReactiveType(method.getReturnType())) {
            return (Class<T>)ReflectionUtil.getRawType(ReflectionUtil.getTypeOfFluxOrMono(method));
        }
        return (Class<T>)method.getReturnType();
    }

    private HttpResponseStatus getSuccessStatus(JaxRsResource resource) {
        SuccessStatus successStatus = ReflectionUtil.getAnnotation(resource.getResourceMethod(), SuccessStatus.class);
        if (successStatus != null) {
            return HttpResponseStatus.valueOf(successStatus.value());
        }

        if (resource.getHttpMethod().equals(HttpMethod.POST)) {
            return HttpResponseStatus.CREATED;
        }

        return HttpResponseStatus.OK;
    }
}
