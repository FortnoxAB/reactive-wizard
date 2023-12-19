package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Sets;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.timeout.ReadTimeoutException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import se.fortnox.reactivewizard.jaxrs.ByteBufCollector;
import se.fortnox.reactivewizard.jaxrs.FieldError;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.FluxRxConverter;
import se.fortnox.reactivewizard.util.JustMessageException;
import se.fortnox.reactivewizard.util.ReactiveDecorator;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static reactor.core.Exceptions.isRetryExhausted;
import static reactor.core.publisher.Mono.just;

public class HttpClient implements InvocationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClient.class);
    private static final Class BYTEARRAY_TYPE = (new byte[0]).getClass();
    private static final String COOKIE = "Cookie";
    private static final String QUOTE = "\"";
    private static final String ERROR_CALLING_OTHER_SERVICE = """
        Error calling other service:
        \tResponse Status: %d
        \tURL: %s
        \tRequest Headers: %s
        \tResponse Headers: %s
        \tData: %s""";

    protected final InetSocketAddress serverInfo;
    protected final HttpClientConfig config;
    private final ByteBufCollector collector;
    private final RequestParameterSerializers requestParameterSerializers;
    private final Set<PreRequestHook> preRequestHooks;
    private final ReactorRxClientProvider clientProvider;
    private final ObjectMapper objectMapper;
    private final RequestLogger requestLogger;
    private final Map<Class<?>, List<HttpClient.BeanParamProperty>> beanParamCache = new ConcurrentHashMap<>();
    private final Map<Method, JaxRsMeta> jaxRsMetaMap = new ConcurrentHashMap<>();
    private int timeout = 10;
    private TemporalUnit timeoutUnit = ChronoUnit.SECONDS;
    private final Duration retryDuration;

    @Inject
    public HttpClient(HttpClientConfig config,
                      ReactorRxClientProvider clientProvider,
                      ObjectMapper objectMapper,
                      RequestParameterSerializers requestParameterSerializers,
                      Set<PreRequestHook> preRequestHooks,
                      RequestLogger requestLogger
    ) {
        this.config = config;
        this.clientProvider = clientProvider;
        this.objectMapper = objectMapper;
        this.requestLogger = requestLogger;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var constraints = StreamReadConstraints.builder()
            .maxStringLength(20_000_000)
            .build();
        this.objectMapper.getFactory()
            .setStreamReadConstraints(constraints);
        this.requestParameterSerializers = requestParameterSerializers;

        serverInfo = new InetSocketAddress(config.getHost(), config.getPort());
        collector = new ByteBufCollector(config.getMaxResponseSize());
        this.preRequestHooks = preRequestHooks;
        this.retryDuration = Duration.ofMillis(config.getRetryDelayMs());
    }

    public HttpClient(HttpClientConfig config) {
        this(config, new ReactorRxClientProvider(config, new HealthRecorder()), new ObjectMapper(), new RequestParameterSerializers(),
            emptySet(), new RequestLogger());
    }

    public static void setTimeout(Object proxy, int timeout, ChronoUnit timeoutUnit) {
        ifHttpClientDo(proxy, httpClient -> httpClient.setTimeout(timeout, timeoutUnit));
    }

    public void setTimeout(int timeout, ChronoUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    public static void markHeaderAsSensitive(Object proxy, String header) {
        markHeadersAsSensitive(proxy, singleton(header));
    }

    public static void markHeadersAsSensitive(Object proxy, Set<String> headers) {
        ifHttpClientDo(proxy, httpClient -> httpClient.addSensitiveHeaders(headers));
    }

    private static void ifHttpClientDo(Object proxy, Consumer<HttpClient> consumer) {
        if (Proxy.isProxyClass(proxy.getClass())) {
            Object handler = Proxy.getInvocationHandler(proxy);
            if (handler instanceof HttpClient httpClient) {
                consumer.accept(httpClient);
            }
        }
    }

    public void addSensitiveHeaders(Set<String> headers) {
        headers.forEach(requestLogger::addRedactedHeaderClient);
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> jaxRsInterface) {
        return (T) Proxy.newProxyInstance(jaxRsInterface.getClassLoader(), new Class[]{jaxRsInterface}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) {
        if (arguments == null) {
            arguments = new Object[0];
        }

        RequestBuilder request = createRequest(method, arguments);

        addDevOverrides(request);
        addAuthenticationHeaders(request);

        reactor.netty.http.client.HttpClient rxClient = clientProvider.clientFor(request.getServerInfo());

        Mono<RwHttpClientResponse> response = request.submit(rxClient, request);

        Class<?> returnType = method.getReturnType();

        Mono<Response<Flux<?>>> responseWithResult = createResponseWithResult(method, request, response);
        Flux<?> resultOnly = responseWithResult.flatMapMany(Response::getBody);

        Function<Flux, Object> converter = FluxRxConverter.converterFromFlux(returnType);
        return ReactiveDecorator.decorated(converter.apply(resultOnly), responseWithResult);
    }

    private Mono<Response<Flux<?>>> createResponseWithResult(Method method, RequestBuilder request, Mono<RwHttpClientResponse> responseMono) {
        boolean isSingle = FluxRxConverter.isSingleType(method.getReturnType());
        Mono<Response<Flux<?>>> result = responseMono.flatMap(response -> {
            Mono<Response<Flux<?>>> error = handleError(request, response);
            if (error != null) {
                return error;
            }
            Flux<Object> body;
            if (isSingle) {
                body = parseResponseSingle(method, response);
            } else {
                body = parseResponseStream(method, response);
            }
            body = body.onErrorResume(e -> convertError(request, e));
            return Mono.just(new Response<>(response.getHttpClientResponse(), body));
        });
        return withRetry(request, measure(request, result).timeout(Duration.of(timeout, timeoutUnit)))
            .onErrorResume(e -> convertError(request, e));
    }

    private static <T> Mono<Response<T>> flattenResponse(Mono<Response<Flux<T>>> responseFlux) {
        return responseFlux.flatMap(response -> response.getBody()
            .singleOrEmpty()
            .map(response::withBody)
            .switchIfEmpty(Mono.fromCallable(response::withNoBody))
        );
    }

    /**
     * Should be used with a Flux coming directly from another api-call to get access to metadata, such as status and header.
     *
     * @param source the source observable, must be observable returned from api call
     * @param <T>    the type of data that should be returned in the call
     * @return an observable that along with the data passes the response object from netty
     */
    public static <T> Mono<Response<Flux<T>>> getFullResponse(Flux<T> source) {
        Optional<Mono<Response<Flux<T>>>> responseFlux = ReactiveDecorator.getDecoration(source);
        if (responseFlux.isEmpty()) {
            throw new IllegalArgumentException("Must be used with Flux returned from api call");
        }
        return responseFlux.get();
    }

    /**
     * Should be used with a Mono coming directly from another api-call to get access to metadata, such as status and header.
     *
     * @param source the source observable, must be observable returned from api call
     * @param <T>    the type of data that should be returned in the call
     * @return an observable that along with the data passes the response object from netty
     */
    public static <T> Mono<Response<T>> getFullResponse(Mono<T> source) {
        Optional<Mono<Response<Flux<T>>>> responseFlux = ReactiveDecorator.getDecoration(source);
        if (responseFlux.isEmpty()) {
            throw new IllegalArgumentException("Must be used with Mono returned from api call");
        }
        return flattenResponse(responseFlux.get());
    }

    private <T> Mono<T> convertError(RequestBuilder fullReq, Throwable throwable) {
        String request = format("%s, headers: %s", fullReq.getFullUrl(), requestLogger.getHeaderValuesOrRedactClient(fullReq.getHeaders()));
        LOG.warn("Failed request. Url: {}", request, throwable);

        if (isRetryExhausted(throwable)) {
            throwable = throwable.getCause();
        }

        if (throwable instanceof TimeoutException || throwable instanceof ReadTimeoutException) {
            String message = format("Timeout after %d ms calling %s", Duration.of(timeout, timeoutUnit).toMillis(), request);
            return Mono.error(new WebException(GATEWAY_TIMEOUT, new JustMessageException(message), false));
        } else if (!(throwable instanceof WebException)) {
            String message = format("Error calling %s", request);
            return Mono.error(new WebException(INTERNAL_SERVER_ERROR, new JustMessageException(message, throwable), false));
        }
        return Mono.error(throwable);
    }

    protected Flux<Object> parseResponseSingle(Method method, RwHttpClientResponse response) {
        if (expectsByteArrayResponse(method)) {
            return Flux.from(collector.collectBytes(response.getContent()));
        }
        return Flux.from(collector.collectString(response.getContent())
            .flatMap(stringContent -> this.deserialize(method, stringContent)));
    }

    private String resolveContentType(Method method, RwHttpClientResponse response) {
        String contentType = Optional.ofNullable(response.getHttpClientResponse().responseHeaders().get(CONTENT_TYPE))
            .map(HttpUtil::getMimeType)
            .map(CharSequence::toString)
            .orElse(null);

        // Override response content-type if resource method is annotated with a non-empty @Produces
        JaxRsMeta jaxRsMeta = new JaxRsMeta(method);
        String overridingContentType = jaxRsMeta.getProduces();
        if (!isNullOrEmpty(overridingContentType) && jaxRsMeta.isProducesAnnotationPresent()) {
            if (!overridingContentType.equals(contentType)) {
                LOG.warn("Content-Type {} does not match the Content-Type {} when parsing response stream from {}::{}, continuing with Content-Type {}",
                    contentType, overridingContentType, method.getDeclaringClass().getCanonicalName(), method.getName(), overridingContentType);
            }
            contentType = overridingContentType;
        }

        return contentType;
    }

    protected Flux<Object> parseResponseStream(Method method, RwHttpClientResponse response) {
        String contentType = resolveContentType(method, response);

        if (APPLICATION_JSON.equals(contentType)) {
            JsonArrayDeserializer deserializer = new JsonArrayDeserializer(objectMapper, method);
            return response.getContent().asByteArray().concatMap(deserializer::process);
        } else {
            return response.getContent().asByteArray().cast(Object.class);
        }
    }

    private boolean expectsByteArrayResponse(Method method) {
        Type type = ReflectionUtil.getTypeOfFluxOrMono(method);
        return type.equals(BYTEARRAY_TYPE);
    }

    private void addDevOverrides(RequestBuilder fullRequest) {
        if (config.getDevServerInfo() != null) {
            fullRequest.setServerInfo(config.getDevServerInfo());
        }

        if (config.getDevCookie() != null) {
            String cookie = fullRequest.getHeaders().get(COOKIE) + ";" + config.getDevCookie();
            fullRequest.getHeaders().remove(COOKIE);
            fullRequest.addHeader(COOKIE, cookie);
        }

        if (config.getDevHeaders() != null) {
            config.getDevHeaders().forEach(fullRequest::addHeader);
        }
    }

    /**
     * Add Authorization-headers if the config contains username and password.
     */
    private void addAuthenticationHeaders(RequestBuilder request) {
        if (config.getBasicAuth() == null) {
            return;
        }

        String basicAuthString = createBasicAuthString();
        request.addHeader("Authorization", basicAuthString);
    }

    /**
     * Create basic auth string based on config.
     *
     * @return the auth string
     */
    private String createBasicAuthString() {
        Charset charset = StandardCharsets.ISO_8859_1;
        String authString = config.getBasicAuth().getUsername() + ":" + config.getBasicAuth().getPassword();
        byte[] encodedAuth = Base64.getEncoder().encode(authString.getBytes(charset));
        return "Basic " + new String(encodedAuth);
    }

    protected <T> Mono<T> measure(RequestBuilder fullRequest, Mono<T> output) {
        return Metrics.get("OUT_res:" + fullRequest.getKey()).measure(output);
    }

    protected Mono<Response<Flux<?>>> withRetry(RequestBuilder fullReq, Mono<Response<Flux<?>>> responseMono) {
        return responseMono.retryWhen(Retry.backoff(config.getRetryCount(), this.retryDuration).filter(throwable -> {
            if (fullReq.getHttpMethod().equals(POST)) {
                // Don't retry if it was a POST, as it is not idempotent
                return false;
            }
            if (throwable instanceof TimeoutException) {
                return false;
            }
            Throwable cause = throwable.getCause();
            if (throwable instanceof JsonMappingException || cause instanceof JsonMappingException) {
                // Do not retry when deserialization failed
                return false;
            }
            if (!(throwable instanceof WebException)) {
                return true;
            }
            if (((WebException) throwable).getStatus().code() >= 500) {

                // Log the error on every retry.
                LOG.info(format("Will retry because an error occurred. %s, headers: %s",
                    fullReq.getFullUrl(),
                    requestLogger.getHeaderValuesOrRedactClient(fullReq.getHeaders())), throwable);

                // Retry if it's 500+ error
                return true;
            }
            // Don't retry if it is a 400 error or something like that
            return false;
        }));
    }

    protected void addContent(Method method, Object[] arguments, RequestBuilder requestBuilder) {
        if (!requestBuilder.canHaveBody() || requestBuilder.hasContent()) {
            return;
        }
        Class<?>[] types = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            Object value = arguments[i];
            if (value == null) {
                continue;
            }
            FormParam formParam = getFormParam(annotations[i]);
            if (formParam != null) {
                addFormParamToOutput(output, value, formParam);
                requestBuilder.getHeaders().put(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            } else if (isBodyArg(types[i], annotations[i])) {
                try {
                    if (!requestBuilder.getHeaders().containsKey(CONTENT_TYPE)) {
                        requestBuilder.getHeaders().put(CONTENT_TYPE, APPLICATION_JSON);
                    }
                    if (requestBuilder.getHeaders().get(CONTENT_TYPE).startsWith(APPLICATION_JSON)) {
                        requestBuilder.setContent(objectMapper.writeValueAsBytes(value));
                    } else {
                        if (value instanceof String string) {
                            requestBuilder.setContent(string);
                            return;
                        } else if (value instanceof byte[] bytes) {
                            requestBuilder.setContent(bytes);
                            return;
                        } else if (value instanceof Publisher publisher) {
                            requestBuilder.setContent(publisher);
                            return;
                        }
                        throw new IllegalArgumentException("When content type is not " + APPLICATION_JSON
                            + " the body param must be String, byte[] or Publisher<? extends byte[]>, but was "
                            + value.getClass());
                    }
                    return;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (!output.isEmpty()) {
            requestBuilder.setContent(output.toString());
        }
    }

    protected void addFormParamToOutput(StringBuilder output, Object value, FormParam formParam) {
        if (!output.isEmpty()) {
            output.append("&");
        }
        output.append(formParam.value()).append("=").append(urlEncode(value.toString()));
    }

    private FormParam getFormParam(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof FormParam formParam) {
                return formParam;
            }
        }
        return null;
    }

    protected boolean isBodyArg(@SuppressWarnings("unused") Class<?> cls, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof QueryParam || annotation instanceof PathParam || annotation instanceof HeaderParam || annotation instanceof CookieParam) {
                return false;
            }
        }

        return requestParameterSerializers.getSerializer(cls) == null;
    }

    protected Mono<Response<Flux<?>>> handleError(RequestBuilder request, RwHttpClientResponse response) {
        HttpResponseStatus status = response.getHttpClientResponse().status();
        if (status.code() >= 400) {
            return collector.collectString(response.getContent()).onErrorReturn("").map(data -> {
                String message = format(ERROR_CALLING_OTHER_SERVICE,
                    status.code(),
                    request.getFullUrl(),
                    requestLogger.getHeaderValuesOrRedactClient(request.getHeaders()),
                    formatHeaders(response.getHttpClientResponse()),
                    data);
                Throwable detailedErrorCause = new HttpClient.ThrowableWithoutStack(message);
                HttpClient.DetailedError detailedError = getDetailedError(data, detailedErrorCause);
                String reasonPhrase = detailedError.hasReason() ? detailedError.reason() : status.reasonPhrase();
                HttpResponseStatus responseStatus = new HttpResponseStatus(status
                    .code(),
                    reasonPhrase);

                throw new WebException(responseStatus, detailedError, false, data);
            });
        }
        return null;
    }

    private String formatHeaders(reactor.netty.http.client.HttpClientResponse clientResponse) {
        StringBuilder headers = new StringBuilder();
        clientResponse.responseHeaders().forEach(h -> headers.append(h.getKey()).append('=').append(h.getValue()).append(' '));
        return headers.toString();
    }

    private HttpClient.DetailedError getDetailedError(String data, Throwable cause) {
        HttpClient.DetailedError detailedError = new HttpClient.DetailedError(cause);
        if (data != null && !data.isEmpty()) {
            try {
                objectMapper.readerForUpdating(detailedError).readValue(data);
            } catch (IOException e) {
                detailedError.setMessage(data);
            }
        }
        return detailedError;
    }

    protected JaxRsMeta getJaxRsMeta(Method method) {
        return jaxRsMetaMap.computeIfAbsent(method, JaxRsMeta::new);
    }

    protected RequestBuilder createRequest(Method method, Object[] arguments) {

        JaxRsMeta meta = getJaxRsMeta(method);

        RequestBuilder request = new RequestBuilder(serverInfo, meta.getHttpMethod(), meta.getFullPath());
        request.setUri(getPath(method, arguments, meta));
        setHeaderParams(request, method, arguments);
        addCustomParams(request, method, arguments);

        Consumes consumes = method.getAnnotation(Consumes.class);
        if (consumes != null && consumes.value().length != 0) {
            request.addHeader("Content-Type", consumes.value()[0]);
        }

        applyPreRequestHooks(request);

        addContent(method, arguments, request);

        return request;
    }

    private void applyPreRequestHooks(RequestBuilder request) {
        preRequestHooks.forEach(hook -> hook.apply(request));
    }

    @SuppressWarnings("unchecked")
    private void addCustomParams(RequestBuilder request, Method method, Object[] arguments) {
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            RequestParameterSerializer serializer = requestParameterSerializers.getSerializer(types[i]);
            if (serializer != null) {
                serializer.addParameter(arguments[i], request);
            }
        }
    }

    private void setHeaderParams(RequestBuilder request, Method method, Object[] arguments) {
        Class<?>[] types = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < types.length; i++) {
            Object value = arguments[i];
            if (value == null) {
                continue;
            }
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof HeaderParam headerParam) {
                    request.addHeader(headerParam.value(), serialize(value));
                } else if (annotation instanceof CookieParam cookieParam) {
                    final String currentCookieValue = request.getHeaders().get(COOKIE);
                    final String cookiePart = cookieParam.value() + "=" + serialize(value);
                    if (currentCookieValue != null) {
                        request.addHeader(COOKIE, format("%s; %s", currentCookieValue, cookiePart));
                    } else {
                        request.addHeader(COOKIE, cookiePart);
                    }
                }
            }
        }

        if (isNullOrEmpty(request.getHeaders().get("Host"))) {
            request.addHeader("Host", this.config.getHost());
        }
    }

    protected Mono<Object> deserialize(Method method, String string) {
        if (string == null || string.isEmpty()) {
            return Mono.empty();
        }
        Type type = ReflectionUtil.getTypeOfFluxOrMono(method);

        if (Void.class.equals(type)) {
            return Mono.empty();
        }

        if (String.class.equals(type) && !string.startsWith(QUOTE) && !"null".equalsIgnoreCase(string)) {
            return just(string);
        }

        try {
            JavaType javaType = TypeFactory.defaultInstance().constructType(type);
            ObjectReader reader = objectMapper.readerFor(javaType);
            Object value = reader.readValue(string);
            return Mono.justOrEmpty(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String encode(String path) {
        try {
            return new URI(null, null, path, null, null).toASCIIString().replaceAll("\\+", "%2B");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected String urlEncode(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    protected String getPath(Method method, Object[] arguments, JaxRsMeta meta) {
        String path = meta.getFullPath();

        StringBuilder query = null;
        Class<?>[] types = method.getParameterTypes();
        List<Object> args = new ArrayList<>(asList(arguments));
        List<Annotation[]> argumentAnnotations = new ArrayList<>(asList(method.getParameterAnnotations()));
        for (int i = 0; i < args.size(); i++) {
            Object value = args.get(i);
            for (Annotation annotation : argumentAnnotations.get(i)) {
                if (annotation instanceof QueryParam queryParam) {
                    if (value == null) {
                        continue;
                    }
                    if (query == null) {
                        query = new StringBuilder(path.contains("?") ? "&" : "?");
                    } else {
                        query.append('&');
                    }
                    query.append(queryParam.value());
                    query.append('=');
                    query.append(urlEncode(serialize(value)));
                } else if (annotation instanceof PathParam pathParam) {
                    if (value == null) {
                        throw new IllegalArgumentException(
                            format("Failed to send http request, unexpected null argument for path param '%s' when calling %s::%s",
                                pathParam.value(),
                                method.getDeclaringClass().getCanonicalName(),
                                method.getName()));
                    }
                    if (path.contains("{" + pathParam.value() + ":.*}")) {
                        path = path.replaceAll("\\{" + pathParam.value() + ":.*\\}", this.encode(this.serialize(value)));
                    } else {
                        path = path.replaceAll("\\{" + pathParam.value() + "\\}", this.urlEncode(this.serialize(value)));
                    }
                } else if (annotation instanceof BeanParam) {
                    if (value == null) {
                        continue;
                    }
                    beanParamCache
                        .computeIfAbsent(types[i], this::getBeanParamGetters)
                        .forEach(beanParamProperty -> {
                            args.add(beanParamProperty.getter.apply(value));
                            argumentAnnotations.add(beanParamProperty.annotations);
                        });
                }
            }
        }
        if (query != null) {
            return path + query;
        }
        return path;
    }

    private List<BeanParamProperty> getBeanParamGetters(Class beanParamType) {
        List<BeanParamProperty> result = new ArrayList<>();
        for (Field field : getDeclaredFieldsFromClassAndAncestors(beanParamType)) {
            Optional<Function<Object, Object>> optionalGetter = ReflectionUtil.getter(beanParamType, field.getName());
            optionalGetter.ifPresent(getter ->
                result.add(new BeanParamProperty(
                    getter,
                    field.getAnnotations()
                )));
        }
        return result;
    }

    /**
     * Recursive function getting all declared fields from the passed in class and its ancestors.
     *
     * @param clazz the clazz fetching fields from
     * @return set of fields
     */
    private static Set<Field> getDeclaredFieldsFromClassAndAncestors(Class clazz) {
        final HashSet<Field> declaredFields = new HashSet<>(asList(clazz.getDeclaredFields()));

        if (clazz.getSuperclass() == null || Object.class.equals(clazz.getSuperclass())) {
            return declaredFields;
        }
        return Sets.union(getDeclaredFieldsFromClassAndAncestors(clazz.getSuperclass()), declaredFields);
    }

    protected String serialize(Object value) {
        if (value instanceof Date date) {
            return String.valueOf(date.getTime());
        }
        if (value.getClass().isArray()) {
            value = asList((Object[]) value);
        }
        if (value instanceof List list) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                Object entryValue = list.get(i);
                stringBuilder.append(entryValue);
                if (i < list.size() - 1) {
                    stringBuilder.append(",");
                }
            }
            return stringBuilder.toString();
        }
        return value.toString();
    }

    protected boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static class DetailedError extends Throwable {
        private int code;
        private String error;
        private String message;
        private FieldError[] fields;

        public DetailedError() {
            this(null);
        }

        public DetailedError(Throwable cause) {
            super(null, cause, false, false);
        }

        @Override
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String reason() {
            return error;
        }

        public boolean hasReason() {
            return code == 0 && error != null;
        }

        public FieldError[] getFields() {
            return fields;
        }

        public void setFields(FieldError[] fields) {
            this.fields = fields;
        }
    }

    public static class ThrowableWithoutStack extends Throwable {
        public ThrowableWithoutStack(String message) {
            super(message, null, false, false);
        }
    }

    private static class BeanParamProperty {
        final Function<Object, Object> getter;
        final Annotation[] annotations;

        public BeanParamProperty(Function<Object, Object> getter, Annotation[] annotations) {
            this.getter = getter;
            this.annotations = annotations;
        }
    }
}
