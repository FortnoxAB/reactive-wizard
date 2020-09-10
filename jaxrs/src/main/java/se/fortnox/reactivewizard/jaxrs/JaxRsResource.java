package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import rx.Observable;
import rx.Single;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactory;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;
import se.fortnox.reactivewizard.jaxrs.response.ResponseDecorator;
import se.fortnox.reactivewizard.util.FluxRxConverter;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Represents a JaxRs resource. Maps to a method of a resource class. Use the call method with an incoming request to
 * invoke the method of the resource. Returns null if the request does not match.
 */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class JaxRsResource<T> implements Comparable<JaxRsResource> {

    private static final RequestLogger REQUEST_LOGGER = new RequestLogger(LoggerFactory.getLogger(JaxRsResource.class));
    private static final Object EMPTY_ARG = new Object();
    private final Pattern                           pathPattern;
    private final Method                            method;
    private final Method                            instanceMethod;
    private final Integer                           paramCount;
    private final List<ParamResolver>               argumentExtractors;
    private final JaxRsResultFactory<T>             resultFactory;
    private final JaxRsMeta                         meta;
    private final Function<Object[], Flux<T>> methodCaller;

    public JaxRsResource(Method method,
        Object resourceInstance,
        ParamResolverFactories paramResolverFactories,
        JaxRsResultFactoryFactory jaxRsResultFactoryFactory,
        JaxRsMeta meta
    ) {
        this.method = method;
        this.meta = meta;
        this.pathPattern = createPathPattern(meta.getFullPath());
        this.paramCount = method.getParameterCount();

        instanceMethod = ReflectionUtil.getInstanceMethod(method, resourceInstance);

        this.argumentExtractors = paramResolverFactories.createParamResolvers(instanceMethod, getConsumes());
        this.resultFactory = jaxRsResultFactoryFactory.createResultFactory(this);
        this.methodCaller = createMethodCaller(method, resourceInstance);
    }

    private static Pattern createPathPattern(String path) {
        // Vars with custom regex, like this: {myvar:myregex}
        path = path.replaceAll("\\{([^}]+):([^}]+)\\}", "(?<$1>$2)");
        // Vars without custom regex, like this: {myvar}
        path = path.replaceAll("\\{([^}]+)\\}", "(?<$1>[^/]+)");
        // Allow trailing slash
        path = "^" + path + "[/\\s]*$";

        return Pattern.compile(path);
    }

    public boolean canHandleRequest(JaxRsRequest request) {
        if (!request.hasMethod(meta.getHttpMethod())) {
            return false;
        }
        if (!request.matchesPath(pathPattern)) {
            return false;
        }
        return true;
    }

    protected Mono<JaxRsResult<T>> call(JaxRsRequest request) {
        return request.loadBody()
            .flatMap(this::resolveArgs)
            .map(this::call);
    }

    private JaxRsResult<T> call(Object[] args) {
        Flux<T> observableOutput = methodCaller.apply(args);
        return resultFactory.create(observableOutput, args);
    }

    @SuppressWarnings("unchecked")
    private Function<Object[], Flux<T>> createMethodCaller(Method method, Object resourceInstance) {
        if (!Observable.class.isAssignableFrom(method.getReturnType()) && !Single.class.isAssignableFrom(method.getReturnType())) {
            throw new IllegalArgumentException(format(
                "Can only serve methods that are reactive. %s had unsupported return type %s",
                method, method.getReturnType()));
        } else {
            return args -> {
                try {
                    Object result = method.invoke(resourceInstance, args);

                    if (result == null) {
                        return Flux.empty();
                    }

                    if (result instanceof Single) {
                        return observableToFlux(((Single<T>)result).toObservable());
                    }

                    return observableToFlux((Observable<T>)result);
                } catch (InvocationTargetException e) {
                    return Flux.error(e.getTargetException());
                } catch (Throwable e) {
                    return Flux.error(e);
                }
            };
        }
    }

    private Flux<T> observableToFlux(Observable<T> result) {
        Flux<T> fluxResult = FluxRxConverter.observableToFlux(result);

        // This part should be refactored so that this class does not know about decorator, but right now the meta data
        // is lost if not handled here.
        if (result instanceof ResponseDecorator.ObservableWithHeaders) {
            Map<String, String> headers = ((ResponseDecorator.ObservableWithHeaders<T>) result).getHeaders();
            return new ResponseDecorator.FluxWithHeaders<>(fluxResult, headers);
        }

        return fluxResult;
    }

    @SuppressWarnings("unchecked")
    private Mono<Object[]> resolveArgs(JaxRsRequest request) {
        if (argumentExtractors.isEmpty()) {
            return Mono.just(new Object[0]);
        }

        Mono<?>[] obsArgs = new Mono[argumentExtractors.size()];
        for (int i = 0; i < obsArgs.length; i++) {
            obsArgs[i] = argumentExtractors.get(i).resolve(request).defaultIfEmpty(EMPTY_ARG);
        }

        return Mono.zip(asList(obsArgs), array -> {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == EMPTY_ARG) {
                    array[i] = null;
                }
            }
            return array;
        });
    }

    @Override
    public int compareTo(JaxRsResource jaxRsResource) {
        int pathCompare = this.meta.getFullPath().compareTo(jaxRsResource.meta.getFullPath());
        if (pathCompare == 0) {
            return jaxRsResource.paramCount.compareTo(this.paramCount);
        }
        return pathCompare;
    }

    @Override
    public String toString() {
        return format("%1$s\t%2$s (%3$s.%4$s)",
            meta.getHttpMethod(),
            meta.getFullPath(),
            method.getDeclaringClass().getName(),
            method.getName());
    }

    public Method getResourceMethod() {
        return method;
    }

    public Method getInstanceMethod() {
        return instanceMethod;
    }

    public HttpMethod getHttpMethod() {
        return meta.getHttpMethod();
    }

    public String getProduces() {
        return meta.getProduces();
    }

    /**
     * Only supports a single consumes media type for now, because we construct the param extractor at init.
     *
     * @return Annotation value
     */
    private String[] getConsumes() {
        Consumes consumesAnnotation = meta.getConsumes();
        if (consumesAnnotation != null) {
            String[] mediaTypes = consumesAnnotation.value();
            if (mediaTypes.length != 0) {
                return mediaTypes;
            }
        }
        return new String[]{MediaType.APPLICATION_JSON};
    }

    public void log(HttpServerRequest request, HttpServerResponse response, long requestStartTime) {
        REQUEST_LOGGER.log(request, response, requestStartTime);
    }

    public String getPath() {
        return meta.getFullPath();
    }
}
