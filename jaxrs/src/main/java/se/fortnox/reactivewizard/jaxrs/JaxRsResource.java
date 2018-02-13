package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Single;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactory;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static rx.Observable.empty;
import static rx.Observable.just;
import static rx.Observable.zip;

/**
 * Represents a JaxRs resource. Maps to a method of a resource class. Use the call method with an incoming request to
 * invoke the method of the resource. Returns null if the request does not match.
 */
public class JaxRsResource<T> implements Comparable<JaxRsResource> {

    private static final RequestLogger REQUEST_LOGGER = new RequestLogger(LoggerFactory.getLogger(JaxRsResource.class));
    private final Pattern                           pathPattern;
    private final Method                            method;
    private final Method                            instanceMethod;
    private final Integer                           paramCount;
    private final List<ParamResolver>               argumentExtractors;
    private final JaxRsResultFactory<T>             resultFactory;
    private final JaxRsMeta                         meta;
    private final Function<Object[], Observable<T>> methodCaller;

    public JaxRsResource(Method method,
        Object resourceInstance,
        ParamResolverFactories paramResolverFactories,
        JaxRsResultFactoryFactory jaxRsResultFactoryFactory,
        BlockingResourceScheduler blockingResourceScheduler, JaxRsMeta meta
    ) {
        this.method = method;
        this.meta = meta;
        this.pathPattern = createPathPattern(meta.getFullPath());
        this.paramCount = method.getParameterCount();

        instanceMethod = ReflectionUtil.getInstanceMethod(method, resourceInstance);

        this.argumentExtractors = paramResolverFactories.createParamResolvers(instanceMethod, getConsumes());
        this.resultFactory = jaxRsResultFactoryFactory.createResultFactory(this);
        this.methodCaller = createMethodCaller(method, resourceInstance, blockingResourceScheduler);
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

    protected Observable<JaxRsResult<T>> call(JaxRsRequest request) {
        return request.loadBody()
            .flatMap(this::resolveArgs)
            .map(this::call)
            .first();
    }

    private JaxRsResult<T> call(Object[] args) {
        Observable<T> observableOutput = methodCaller.apply(args);
        return resultFactory.create(observableOutput, args);
    }

    @SuppressWarnings("unchecked")
    private Function<Object[], Observable<T>> createMethodCaller(Method method, Object resourceInstance, BlockingResourceScheduler blockingResourceScheduler) {
        if (!Observable.class.isAssignableFrom(method.getReturnType()) && !Single.class.isAssignableFrom(method.getReturnType())) {
            return args -> Observable.<T>create(s -> {
                try {
                    s.onNext((T)method.invoke(resourceInstance, args));
                    s.onCompleted();
                } catch (InvocationTargetException e) {
                    s.onError(e.getTargetException());
                } catch (Throwable e) {
                    s.onError(e);
                }
            }).subscribeOn(blockingResourceScheduler);
        } else {
            return args -> {
                try {
                    Object result = method.invoke(resourceInstance, args);

                    if (result == null) {
                        return empty();
                    }

                    if (result instanceof Single) {
                        return ((Single<T>)result).toObservable();
                    }

                    return (Observable<T>)result;
                } catch (InvocationTargetException e) {
                    return Observable.error(e.getTargetException());
                } catch (Throwable e) {
                    return Observable.error(e);
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    private Observable<Object[]> resolveArgs(JaxRsRequest request) {
        if (argumentExtractors.isEmpty()) {
            return just(new Object[0]);
        }

        Observable<?>[] obsArgs = new Observable[argumentExtractors.size()];
        for (int i = 0; i < obsArgs.length; i++) {
            obsArgs[i] = argumentExtractors.get(i).resolve(request).firstOrDefault(null);
        }

        return zip(asList(obsArgs), array -> array).first();
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
        return String.format("%1$s\t%2$s (%3$s.%4$s)",
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

    public void log(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response, long requestStartTime) {
        REQUEST_LOGGER.log(request, response, requestStartTime);
    }

    public String getPath() {
        return meta.getFullPath();
    }
}
