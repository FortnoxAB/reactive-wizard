package se.fortnox.reactivewizard.reactorclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;
import se.fortnox.reactivewizard.client.HttpClientConfig;
import se.fortnox.reactivewizard.client.PreRequestHook;
import se.fortnox.reactivewizard.client.RequestBuilder;
import se.fortnox.reactivewizard.client.RequestParameterSerializer;
import se.fortnox.reactivewizard.client.RequestParameterSerializers;
import se.fortnox.reactivewizard.jaxrs.FieldError;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.JustMessageException;
import se.fortnox.reactivewizard.util.ReflectionUtil;
import se.fortnox.reactivewizard.util.rx.RetryWithDelayFlux;

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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

public class ReactorHttpClient implements InvocationHandler {
    private static final Logger LOG            = LoggerFactory.getLogger(ReactorHttpClient.class);
    private static final Class  BYTEARRAY_TYPE = (new byte[0]).getClass();

    protected final InetSocketAddress                                        serverInfo;
    protected final HttpClientConfig                                         config;
    private final   ByteBufferCollector                                      collector;
    private final   RequestParameterSerializers                              requestParameterSerializers;
    private final   Set<PreRequestHook>                                      preRequestHooks;
    private         ReactorRxClientProvider                                  clientProvider;
    private         ObjectMapper                                             objectMapper;
    private         int                                                      timeout        = 10;
    private         TimeUnit                                                 timeoutUnit    = TimeUnit.SECONDS;
    private final   Map<Class<?>, List<ReactorHttpClient.BeanParamProperty>> beanParamCache = new HashMap<>();
    private final   Map<Method, JaxRsMeta>                                   jaxRsMetaMap   = new ConcurrentHashMap<>();

    @Inject
    public ReactorHttpClient(HttpClientConfig config,
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
        collector = new ByteBufferCollector(config.getMaxResponseSize());
        this.preRequestHooks = preRequestHooks;
    }

    public ReactorHttpClient(HttpClientConfig config) {
        this(config, new ReactorRxClientProvider(config, new HealthRecorder()), new ObjectMapper(), new RequestParameterSerializers(), emptySet());
    }

    public static Mono<String> get(String url) {
        try {
            URL urlObj = new URL(url);

            return reactor.netty.http.client.HttpClient.create()
                .get()
                .uri(urlObj.toString())
                .responseContent()
                .aggregate()
                .asString();

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setTimeout(Object proxy, int timeout, TimeUnit timeoutUnit) {
        if (Proxy.isProxyClass(proxy.getClass())) {
            Object handler = Proxy.getInvocationHandler(proxy);
            if (handler instanceof ReactorHttpClient) {
                ((ReactorHttpClient)handler).setTimeout(timeout, timeoutUnit);
            }
        }
    }

    public void setTimeout(int timeout, TimeUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    public static <T> T create(HttpClientConfig config, ReactorRxClientProvider clientProvider, ObjectMapper objectMapper, Class<T> jaxRsInterface) {
        return new ReactorHttpClient(config, clientProvider, objectMapper, new RequestParameterSerializers(), null).create(jaxRsInterface);
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

        ReactorRequestBuilder request = createRequest(method, arguments);

        addDevOverrides(request);
        addAuthenticationHeaders(request);

        reactor.netty.http.client.HttpClient rxClient = clientProvider.clientFor(request.getServerInfo());

        reactor.netty.http.client.HttpClient.ResponseReceiver<?> response = ReactorHttpClient.submit(rxClient, request);

        Flux<?> output = null;

        if (expectsRawResponse(method)) {
            output =
                Flux.from(response
                    .responseSingle((realResponse, content) -> content.asString().map(string -> new RwHttpClientResponse(realResponse, string))));
        } else if (expectsByteArrayResponse(method)) {
            output = response.response((httpClientResponse, content) -> {
                if (httpClientResponse.status().code() >= 400) {
                    return collector.collectString(content)
                        .map(data -> handleError(request, httpClientResponse, data).getBytes());
                }
                return collector.collectBytes(content);
            });
        } else {
            output = response.response((httpClientResponse, byteBufFlux) ->
                parseResponse(method, request, httpClientResponse, byteBufFlux));
        }

        output = timeout(output);
        output = withRetry(request, output).onErrorResume(e -> convertError(request, e));
        output = measure(request, output);


        if (Single.class.isAssignableFrom(method.getReturnType())) {
            return RxReactiveStreams.toSingle(output);
        } else if (Observable.class.isAssignableFrom(method.getReturnType())) {
            return RxReactiveStreams.toObservable(output);
        } else if (Mono.class.isAssignableFrom(method.getReturnType())) {
            return Mono.from(output);
        }
        return output;
    }

    private Flux<?> timeout(Flux<?> output) {
        Duration duration = null;
        switch (this.timeoutUnit) {
            case NANOSECONDS:
                duration = Duration.ofNanos(timeout);
                break;
            case MICROSECONDS:
                duration = Duration.of(timeout, ChronoUnit.MICROS);
                break;
            case MILLISECONDS:
                duration = Duration.ofMillis(timeout);
                break;
            case SECONDS:
                duration = Duration.ofSeconds(timeout);
                break;
            case MINUTES:
                duration = Duration.ofMinutes(timeout);
                break;
            case HOURS:
                duration = Duration.ofHours(timeout);
                break;
            case DAYS:
                duration = Duration.ofDays(timeout);
                break;
            default:
                duration = Duration.ofMillis(10000);
        }

        return output.timeout(duration);
    }

    private static reactor.netty.http.client.HttpClient.ResponseReceiver<?> submit(
        reactor.netty.http.client.HttpClient client,
        ReactorRequestBuilder requestBuilder) {

        return client
            .headers(entries -> {
                for (Map.Entry<String, String> stringStringEntry : requestBuilder.getHeaders().entrySet()) {
                    entries.set(stringStringEntry.getKey(), stringStringEntry.getValue());
                }

                if (requestBuilder.getContent() != null) {
                    entries.set(CONTENT_LENGTH, requestBuilder.getContent().length());
                }
            })
            .request(requestBuilder.getHttpMethod())
            .uri(requestBuilder.getFullUrl())
            .send(ByteBufFlux.fromString(Mono.just(requestBuilder.getContent())));
    }

    private <T> Flux<T> convertError(RequestBuilder fullReq, Throwable throwable) {
        String request = format("%s, headers: %s", fullReq.getFullUrl(), fullReq.getHeaders().entrySet());
        LOG.warn("Failed request. Url: {}", request, throwable);
        if (throwable instanceof TimeoutException || throwable instanceof ReadTimeoutException) {
            String message = format("Timeout after %d ms calling %s", timeoutUnit.toMillis(timeout), request);
            return Flux.error(new WebException(GATEWAY_TIMEOUT, new JustMessageException(message), false));
        } else if (!(throwable instanceof WebException)) {
            String message = format("Error calling %s", request);
            return Flux.error(new WebException(INTERNAL_SERVER_ERROR, new JustMessageException(message, throwable), false));
        }
        return Flux.error(throwable);
    }

    protected Mono<Object> parseResponse(Method method, RequestBuilder request, reactor.netty.http.client.HttpClientResponse response, ByteBufFlux content) {

        return collector.collectString(content)
            .map(stringContent -> handleError(request, response, stringContent))
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
            String cookie = fullRequest.getHeaders().get("Cookie") + ";" + config.getDevCookie();
            fullRequest.getHeaders().remove("Cookie");
            fullRequest.addHeader("Cookie", cookie);
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

    protected <T> Flux<T> measure(RequestBuilder fullRequest, Flux<T> output) {
        return Metrics.get("OUT_res:" + fullRequest.getKey()).measure(output);
    }

    protected <T> Flux<T> withRetry(RequestBuilder fullReq, Flux<T> response) {
        return response.retryWhen(new RetryWithDelayFlux(config.getRetryCount(), config.getRetryDelayMs(),
            throwable -> {
                if (throwable instanceof TimeoutException) {
                    return false;
                }
                Throwable cause = throwable.getCause();
                if (throwable instanceof JsonMappingException || cause instanceof JsonMappingException) {
                    // Do not retry when deserialization failed
                    return false;
                }
                if (!(throwable instanceof WebException)) {
                    // Don't retry posts when a NoSuchElementException is raised since the request might have ended up at the server the first time
                    if (throwable instanceof NoSuchElementException && POST.equals(fullReq.getHttpMethod())) {
                        return false;
                    }

                    // Retry on system error of some kind, like IO
                    return true;
                }
                if (fullReq.getHttpMethod().equals(POST)) {
                    // Don't retry if it was a POST, as it is not idempotent
                    return false;
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

    private boolean expectsRawResponse(Method method) {
        Type type = ReflectionUtil.getTypeOfObservable(method);

        return type.getTypeName().equals(RwHttpClientResponse.class.getName());
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
            Throwable                detailedErrorCause = new ReactorHttpClient.ThrowableWithoutStack(message);
            ReactorHttpClient.DetailedError detailedError      = getDetailedError(data, detailedErrorCause);
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

    private ReactorHttpClient.DetailedError getDetailedError(String data, Throwable cause) {
        ReactorHttpClient.DetailedError detailedError = new ReactorHttpClient.DetailedError(cause);
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

    protected ReactorRequestBuilder createRequest(Method method, Object[] arguments) {

        JaxRsMeta meta = getJaxRsMeta(method);

        ReactorRequestBuilder request = new ReactorRequestBuilder(serverInfo, meta.getHttpMethod(), meta.getFullPath());
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
                    final String currentCookieValue = request.getHeaders().get("Cookie");
                    final String cookiePart         = ((CookieParam)annotation).value() + "=" + serialize(value);
                    if (currentCookieValue != null) {
                        request.addHeader("Cookie", format("%s; %s", currentCookieValue, cookiePart));
                    } else {
                        request.addHeader("Cookie", cookiePart);
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
        try {
            JavaType     javaType = TypeFactory.defaultInstance().constructType(type);
            ObjectReader reader   = objectMapper.readerFor(javaType);
            return Mono.just(reader.readValue(string));
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

    private List<ReactorHttpClient.BeanParamProperty> getBeanParamGetters(Class beanParamType) {
        List<ReactorHttpClient.BeanParamProperty> result = new ArrayList<>();
        for (Field field : beanParamType.getDeclaredFields()) {
            Optional<Function<Object, Object>> getter = ReflectionUtil.getter(beanParamType, field.getName());
            if (getter.isPresent()) {
                result.add(new ReactorHttpClient.BeanParamProperty(
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
}
