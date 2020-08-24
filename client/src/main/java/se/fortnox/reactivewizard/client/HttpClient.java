package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.ReadTimeoutException;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.retry.Retry;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;
import se.fortnox.reactivewizard.jaxrs.ByteBufCollector;
import se.fortnox.reactivewizard.jaxrs.FieldError;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import se.fortnox.reactivewizard.metrics.PublisherMetrics;
import se.fortnox.reactivewizard.util.JustMessageException;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static reactor.core.Exceptions.isRetryExhausted;

public class HttpClient implements InvocationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClient.class);
    private static final Class  BYTEARRAY_TYPE = (new byte[0]).getClass();
    public static final String COOKIE = "Cookie";

    protected final InetSocketAddress                                        serverInfo;
    protected final HttpClientConfig                                         config;
    private final   ByteBufCollector                                         collector;
    private final   RequestParameterSerializers                              requestParameterSerializers;
    private final   Set<PreRequestHook>                                      preRequestHooks;
    private final   ReactorRxClientProvider                                  clientProvider;
    private final   ObjectMapper                                             objectMapper;
    private final   Map<Class<?>, List<HttpClient.BeanParamProperty>>        beanParamCache = new HashMap<>();
    private final   Map<Method, JaxRsMeta>                                   jaxRsMetaMap   = new ConcurrentHashMap<>();
    private         int                                                      timeout        = 10;
    private TemporalUnit timeoutUnit    = ChronoUnit.SECONDS;
    private final Duration retryDuration;

    @Inject
    public HttpClient(HttpClientConfig config,
                             ReactorRxClientProvider clientProvider,
                             ObjectMapper objectMapper,
                             RequestParameterSerializers requestParameterSerializers,
                             Set<PreRequestHook> preRequestHooks
    ) {
        this.config = config;
        this.clientProvider = clientProvider;
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.requestParameterSerializers = requestParameterSerializers;

        serverInfo = new InetSocketAddress(config.getHost(), config.getPort());
        collector = new ByteBufCollector(config.getMaxResponseSize());
        this.preRequestHooks = preRequestHooks;
        this.retryDuration = Duration.ofMillis(config.getRetryDelayMs());
    }

    public HttpClient(HttpClientConfig config) {
        this(config, new ReactorRxClientProvider(config, new HealthRecorder()), new ObjectMapper(), new RequestParameterSerializers(), emptySet());
    }

    public static void setTimeout(Object proxy, int timeout, ChronoUnit timeoutUnit) {
        if (Proxy.isProxyClass(proxy.getClass())) {
            Object handler = Proxy.getInvocationHandler(proxy);
            if (handler instanceof HttpClient) {
                ((HttpClient)handler).setTimeout(timeout, timeoutUnit);
            }
        }
    }

    public void setTimeout(int timeout, ChronoUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> jaxRsInterface) {
        return (T)Proxy.newProxyInstance(jaxRsInterface.getClassLoader(), new Class[]{jaxRsInterface}, this);
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

        Publisher<?> publisher = null;
        AtomicReference<HttpClientResponse> rawResponse = new AtomicReference<>();
        if (expectsByteArrayResponse(method)) {
            publisher = response.flatMap(rwHttpClientResponse -> {
                rawResponse.set(rwHttpClientResponse.getHttpClientResponse());
                if (rwHttpClientResponse.getHttpClientResponse().status().code() >= 400) {
                    return Mono.from(collector.collectString(rwHttpClientResponse.getContent()))
                        .map(data -> handleError(request, rwHttpClientResponse.getHttpClientResponse(), data).getBytes());
                }
                return Mono.from(collector.collectBytes(rwHttpClientResponse.getContent()));
            });
        } else {
            publisher = response.flatMap(rwHttpClientResponse -> {
                rawResponse.set(rwHttpClientResponse.getHttpClientResponse());
                return parseResponse(method, request, rwHttpClientResponse);
            });
        }
        publisher = measure(request, publisher);

        //End of publisher
        Flux<?> flux = Flux.from(publisher);
        flux = flux.timeout(Duration.of(timeout, timeoutUnit));
        publisher = withRetry(request, flux).onErrorResume(e -> convertError(request, e));

        if (Single.class.isAssignableFrom(method.getReturnType())) {
            return new SingleWithResponse(RxReactiveStreams.toSingle(publisher), rawResponse);
        }
        return new ObservableWithResponse(RxReactiveStreams.toObservable(publisher), rawResponse);
    }

    /**
     * Should be used with an observable coming directly from another api-call to get access to meta data, such as status and headers
     * from the response.
     * @param source the source observable, must be observable returned from api call
     * @param <T> the type of data that should be returned in the call
     * @return an observable that along with the data passes the response meta data
     */
    public static <T> Observable<Response<T>> fullResponse(Observable<T> source) {
        if (!(source instanceof ObservableWithResponse)) {
            throw new IllegalArgumentException("Must be used with observable returned from api call");
        }

        return source.map(data -> new Response<>(((ObservableWithResponse<T>)source).getResponse(), data));
    }

    /** Should be used with
     * @param source the source observable, must be observable returned from api call
     * @param <T> the type of data that should be returned in the call
     * @return an observable that along with the data passes the response object from netty
     */
    public static <T> Single<Response<T>> fullResponse(Single<T> source) {
        if (!(source instanceof SingleWithResponse)) {
            throw new IllegalArgumentException("Must be used with single returned from api call");
        }

        return source.map(data -> new Response<>(((SingleWithResponse<T>)source).getResponse(), data));
    }

    private <T> Flux<T> convertError(RequestBuilder fullReq, Throwable throwable) {
        String request = format("%s, headers: %s", fullReq.getFullUrl(), fullReq.getHeaders().entrySet());
        LOG.warn("Failed request. Url: {}", request, throwable);

        if (isRetryExhausted(throwable)) {
            throwable = throwable.getCause();
        }

        if (throwable instanceof TimeoutException || throwable instanceof ReadTimeoutException) {
            String message = format("Timeout after %d ms calling %s", Duration.of(timeout, timeoutUnit).toMillis(), request);
            return Flux.error(new WebException(GATEWAY_TIMEOUT, new JustMessageException(message), false));
        } else if (!(throwable instanceof WebException)) {
            String message = format("Error calling %s", request);
            return Flux.error(new WebException(INTERNAL_SERVER_ERROR, new JustMessageException(message, throwable), false));
        }
        return Flux.error(throwable);
    }

    protected Mono<Object> parseResponse(Method method, RequestBuilder request, RwHttpClientResponse response) {
        return Mono.from(collector.collectString(response.getContent()))
            .map(stringContent -> handleError(request, response.getHttpClientResponse(), stringContent))
            .flatMap(stringContent -> this.deserialize(method, stringContent));
    }

    private boolean expectsByteArrayResponse(Method method) {
        Type type = ReflectionUtil.getTypeOfObservable(method);
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
     * @return Basic auth string based on config
     */
    private String createBasicAuthString() {
        Charset charset     = StandardCharsets.ISO_8859_1;
        String  authString  = config.getBasicAuth().getUsername() + ":" + config.getBasicAuth().getPassword();
        byte[]  encodedAuth = Base64.getEncoder().encode(authString.getBytes(charset));
        return "Basic " + new String(encodedAuth);
    }

    protected <T> Publisher<T> measure(RequestBuilder fullRequest, Publisher<T> output) {
        return PublisherMetrics.get("OUT_res:" + fullRequest.getKey()).measure(output);
    }

    protected <T> Flux<T> withRetry(RequestBuilder fullReq, Flux<T> response) {
        return response.retryWhen(Retry.backoff(config.getRetryCount(), this.retryDuration).filter(throwable -> {
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
            if (((WebException)throwable).getStatus().code() >= 500) {

                // Log the error on every retry.
                LOG.info(format("Will retry because an error occurred. %s, headers: %s",
                    fullReq.getFullUrl(),
                    fullReq.getHeaders().entrySet()), throwable);

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
        Class<?>[]     types       = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        StringBuilder  output      = new StringBuilder();
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
                        requestBuilder.getHeaders().put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
                    }
                    if (requestBuilder.getHeaders().get(CONTENT_TYPE).startsWith(MediaType.APPLICATION_JSON)) {
                        requestBuilder.setContent(objectMapper.writeValueAsBytes(value));
                    } else {
                        if (value instanceof String) {
                            requestBuilder.setContent((String)value);
                            return;
                        } else if (value instanceof byte[]) {
                            requestBuilder.setContent((byte[])value);
                            return;
                        }
                        throw new IllegalArgumentException("When content type is not " + MediaType.APPLICATION_JSON
                            + " the body param must be String or byte[], but was " + value.getClass());
                    }
                    return;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (output.length() > 0) {
            requestBuilder.setContent(output.toString());
        }
    }

    protected void addFormParamToOutput(StringBuilder output, Object value, FormParam formParam) {
        if (output.length() != 0) {
            output.append("&");
        }
        output.append(formParam.value()).append("=").append(urlEncode(value.toString()));
    }

    private FormParam getFormParam(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof FormParam) {
                return (FormParam)annotation;
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
        return true;
    }

    protected String handleError(RequestBuilder request, reactor.netty.http.client.HttpClientResponse clientResponse, String data) {
        if (clientResponse.status().code() >= 400) {
            String message = format("Error calling other service:\n\tResponse Status: %d\n\tURL: %s\n\tRequest Headers: %s\n\tResponse Headers: %s\n\tData: %s",
                clientResponse.status().code(),
                request.getFullUrl(),
                request.getHeaders().entrySet(),
                formatHeaders(clientResponse),
                data);
            Throwable                detailedErrorCause = new HttpClient.ThrowableWithoutStack(message);
            HttpClient.DetailedError detailedError      = getDetailedError(data, detailedErrorCause);
            String                   reasonPhrase       = detailedError.hasReason() ? detailedError.reason() : clientResponse.status().reasonPhrase();
            HttpResponseStatus responseStatus = new HttpResponseStatus(clientResponse.status()
                .code(),
                reasonPhrase);

            throw new WebException(responseStatus, detailedError, false);
        }
        return data;
    }

    private String formatHeaders(reactor.netty.http.client.HttpClientResponse clientResponse) {
        StringBuilder headers = new StringBuilder();
        clientResponse.responseHeaders().forEach(h -> headers.append(h.getKey()).append('=').append(h.getValue()).append(' '));
        return headers.toString();
    }

    private HttpClient.DetailedError getDetailedError(String data, Throwable cause) {
        HttpClient.DetailedError detailedError = new HttpClient.DetailedError(cause);
        if (data != null && data.length() > 0) {
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
        Class<?>[]     types       = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < types.length; i++) {
            Object value = arguments[i];
            if (value == null) {
                continue;
            }
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof HeaderParam) {
                    request.addHeader(((HeaderParam)annotation).value(), serialize(value));
                } else if (annotation instanceof CookieParam) {
                    final String currentCookieValue = request.getHeaders().get(COOKIE);
                    final String cookiePart         = ((CookieParam)annotation).value() + "=" + serialize(value);
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
        Type type = ReflectionUtil.getTypeOfObservable(method);

        if (Void.class.equals(type)) {
            return Mono.empty();
        }

        try {
            JavaType     javaType = TypeFactory.defaultInstance().constructType(type);
            ObjectReader reader   = objectMapper.readerFor(javaType);
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
        try {
            return URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getPath(Method method, Object[] arguments, JaxRsMeta meta) {
        String path = meta.getFullPath();

        StringBuilder  query       = null;
        Class<?>[]     types       = method.getParameterTypes();
        List<Object> args = new ArrayList<>(asList(arguments));
        List<Annotation[]> argumentAnnotations = new ArrayList<>(asList(method.getParameterAnnotations()));
        for (int i = 0; i < args.size(); i++) {
            Object value = args.get(i);
            for (Annotation annotation : argumentAnnotations.get(i)) {
                if (annotation instanceof QueryParam) {
                    if (value == null) {
                        continue;
                    }
                    if (query == null) {
                        query = new StringBuilder(path.contains("?") ? "&" : "?");
                    } else {
                        query.append('&');
                    }
                    query.append(((QueryParam)annotation).value());
                    query.append('=');
                    query.append(urlEncode(serialize(value)));
                } else if (annotation instanceof PathParam) {
                    if (path.contains("{" + ((PathParam)annotation).value() + ":.*}")) {
                        path = path.replaceAll("\\{" + ((PathParam)annotation).value() + ":.*\\}", this.encode(this.serialize(value)));
                    } else {
                        path = path.replaceAll("\\{" + ((PathParam)annotation).value() + "\\}", this.urlEncode(this.serialize(value)));
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
        for (Field field : beanParamType.getDeclaredFields()) {
            Optional<Function<Object, Object>> getter = ReflectionUtil.getter(beanParamType, field.getName());
            if (getter.isPresent()) {
                result.add(new BeanParamProperty(
                    getter.get(),
                    field.getAnnotations()
                ));
            }
        }
        return result;
    }

    protected String serialize(Object value) {
        if (value instanceof Date) {
            return String.valueOf(((Date)value).getTime());
        }
        if (value.getClass().isArray()) {
            value = asList((Object[])value);
        }
        if (value instanceof List) {
            StringBuilder stringBuilder     = new StringBuilder();
            List          list = (List)value;
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
        private int          code;
        private String       error;
        private String       message;
        private FieldError[] fields;

        public DetailedError() {
            this(null);
        }

        public DetailedError(Throwable cause) {
            super(null, cause, false, false);
        }

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

    /**
     * Class used when the full response is needed
     * @param <T> the type of data to be returned
     */
    public static class Response<T> {
        private final T data;
        private final HttpResponseStatus status;
        private final Map<String, String> headers;

        public Response(HttpClientResponse httpClientResponse, T data) {
            this.status = httpClientResponse.status();
            this.headers = ImmutableMap.copyOf(httpClientResponse.responseHeaders());
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public HttpResponseStatus getStatus() {
            return status;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }

    /**
     * Internal class to help support getting the full response as return value when observable is returned
     * @param <T> the type of data to be returned
     */
    private static class ObservableWithResponse<T> extends Observable<T> {

        private final AtomicReference<HttpClientResponse> httpClientResponse;

        protected ObservableWithResponse(Observable<T> inner, AtomicReference<HttpClientResponse> httpClientResponse) {
            super(inner::unsafeSubscribe);
            this.httpClientResponse = httpClientResponse;
        }

        public HttpClientResponse getResponse() {
            if (httpClientResponse.get() == null) {
                throw new IllegalStateException("This method can only be called after the response has been received");
            }
            return httpClientResponse.get();
        }
    }

    /**
     * Internal class to help support getting the full response as return value when Single is returned
     * @param <T> the type of data to be returned
     */
    private static class SingleWithResponse<T> extends Single<T> {

        private final AtomicReference<HttpClientResponse> httpClientResponse;

        protected SingleWithResponse(Single<T> inner, AtomicReference<HttpClientResponse> httpClientResponse) {
            super((OnSubscribe<T>)inner::subscribe);
            this.httpClientResponse = httpClientResponse;
        }

        public HttpClientResponse getResponse() {
            if (httpClientResponse.get() == null) {
                throw new IllegalStateException("This method can only be called after the response has been received");
            }
            return httpClientResponse.get();
        }
    }
}
