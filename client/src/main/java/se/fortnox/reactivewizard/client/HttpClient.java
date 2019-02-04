package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Single;
import se.fortnox.reactivewizard.jaxrs.ByteBufCollector;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.logging.LoggingContext;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.ReflectionUtil;
import se.fortnox.reactivewizard.util.rx.RetryWithDelay;

import javax.inject.Inject;
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static rx.Observable.just;

public class HttpClient implements InvocationHandler {
    private static final Logger           LOG            = LoggerFactory.getLogger(HttpClient.class);
    private static final Class            BYTEARRAY_TYPE = (new byte[0]).getClass();

    protected final InetSocketAddress           serverInfo;
    protected final HttpClientConfig            config;
    private final   ByteBufCollector            collector;
    private final   RequestParameterSerializers requestParameterSerializers;
    private final   Set<PreRequestHook>         preRequestHooks;
    private         RxClientProvider            clientProvider;
    private         ObjectMapper                objectMapper;
    private         int                         timeout = 10;
    private         TimeUnit                    timeoutUnit = TimeUnit.SECONDS;

    @Inject
    public HttpClient(HttpClientConfig config,
        RxClientProvider clientProvider,
        ObjectMapper objectMapper,
        RequestParameterSerializers requestParameterSerializers,
        Set<PreRequestHook> preRequestHooks
    ) {
        this.config = config;
        this.clientProvider = clientProvider;
        this.objectMapper = objectMapper;
        this.requestParameterSerializers = requestParameterSerializers;

        serverInfo = new InetSocketAddress(config.getHost(), config.getPort());
        collector = new ByteBufCollector(config.getMaxResponseSize());
        this.preRequestHooks = preRequestHooks;
    }

    public HttpClient(HttpClientConfig config) {
        this(config, new RxClientProvider(config, new HealthRecorder()), new ObjectMapper(), new RequestParameterSerializers(), emptySet());
    }

    public static Observable<String> get(String url) {
        try {
            URL urlObj = new URL(url);
            return io.reactivex.netty.protocol.http.client.HttpClient.newClient(urlObj.getHost(), urlObj.getPort())
                .createGet(urlObj.getPath())
                .flatMap(HttpClientResponse::getContent)
                .map(buf -> buf.toString(Charset.defaultCharset()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setTimeout(Object proxy, int timeout, TimeUnit timeoutUnit) {
        if (Proxy.isProxyClass(proxy.getClass())) {
            Object handler = Proxy.getInvocationHandler(proxy);
            if (handler instanceof HttpClient) {
                ((HttpClient)handler).setTimeout(timeout, timeoutUnit);
            }
        }
    }

    public void setTimeout(int timeout, TimeUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    public static <T> T create(HttpClientConfig config, RxClientProvider clientProvider, ObjectMapper objectMapper, Class<T> jaxRsInterface) {
        return new HttpClient(config, clientProvider, objectMapper, new RequestParameterSerializers(), null).create(jaxRsInterface);
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

        RequestBuilder fullReq = createRequest(method, arguments);

        addDevOverrides(fullReq);

        final io.reactivex.netty.protocol.http.client.HttpClient<ByteBuf, ByteBuf> rxClient = clientProvider.clientFor(fullReq.getServerInfo());

        if (LOG.isDebugEnabled()) {
            LOG.debug(fullReq + " with headers " + fullReq.getHeaders().entrySet());
        }

        final Observable<HttpClientResponse<ByteBuf>> resp = fullReq
            .submit(rxClient)
            .timeout(timeout, timeoutUnit)
            //Forcing the stream to be not empty. If empty then it will be retried through the standard retry logic
            .single();

        Observable<?> output = null;
        if (expectsRawResponse(method)) {
            output = resp;
        } else {
            output = resp.flatMap(r -> parseResponse(method, fullReq, r));
        }

        output = withRetry(fullReq, output).doOnError(e -> logFailedRequest(fullReq, e));
        output = LoggingContext.transfer(output);
        output = measure(fullReq, output);

        if (Single.class.isAssignableFrom(method.getReturnType())) {
            return output.toSingle();
        } else {
            return output;
        }
    }

    private void logFailedRequest(RequestBuilder fullReq, Throwable throwable) {
        LOG.warn("Failed request. Url: {}, headers: {}", fullReq.getFullUrl(), fullReq.getHeaders().entrySet(), throwable);
    }

    protected Observable<?> parseResponse(Method method, RequestBuilder request, HttpClientResponse<ByteBuf> response) {
        final Observable<String> body = collector.collectString(response.getContent()).singleOrDefault("");

        if (expectsByteArrayResponse(method)) {
            if (response.getStatus().code() >= 400) {
                return body.map(data -> handleError(request, response, data));
            }

            return collector.collectBytes(response.getContent());
        }

        return body
            .map(data -> handleError(request, response, data))
            .flatMap(str -> deserialize(method, str));
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

        if (config.getDevHeaders() != null && config.getDevHeaders() != null) {
            config.getDevHeaders().forEach(fullRequest::addHeader);
        }
    }

    protected <T> Observable<T> measure(RequestBuilder fullRequest, Observable<T> output) {
        return Metrics.get("OUT_res:" + fullRequest.getKey()).measure(output);
    }

    protected <T> Observable<T> withRetry(RequestBuilder fullReq, Observable<T> response) {
        return response.retryWhen(new RetryWithDelay(config.getRetryCount(), config.getRetryDelayMs(),
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
                    // Retry if it's 500+ error
                    return true;
                }
                // Don't retry if it is a 400 error or something like that
                return false;
            }));
    }

    private boolean expectsRawResponse(Method method) {
        Type type = ReflectionUtil.getTypeOfObservable(method);

        return (type instanceof ParameterizedType && ((ParameterizedType)type).getRawType().equals(HttpClientResponse.class));
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
                    requestBuilder.setContent(objectMapper.writeValueAsBytes(value));
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

    protected String handleError(RequestBuilder request, HttpClientResponse<ByteBuf> clientResponse, String data) {
        if (clientResponse.getStatus().code() >= 400) {
            String message = format("Error calling other service:\n\tResponse Status: %d\n\tURL: %s\n\tRequest Headers: %s\n\tResponse Headers: %s\n\tData: %s",
                clientResponse.getStatus().code(),
                request.getFullUrl(),
                request.getHeaders().entrySet(),
                formatHeaders(clientResponse),
                data);
            Throwable     detailedErrorCause = new ThrowableWithoutStack(message);
            DetailedError detailedError      = getDetailedError(data, detailedErrorCause);
            String        reasonPhrase       = detailedError.hasReason() ? detailedError.reason() : clientResponse.getStatus().reasonPhrase();
            HttpResponseStatus responseStatus = new HttpResponseStatus(clientResponse.getStatus()
                .code(),
                reasonPhrase);

            throw new WebException(responseStatus, detailedError, false);
        }
        return data;
    }

    private String formatHeaders(HttpClientResponse<ByteBuf> clientResponse) {
        StringBuilder headers = new StringBuilder();
        clientResponse.headerIterator().forEachRemaining(h -> headers.append(h.getKey()).append('=').append(h.getValue()).append(' '));
        return headers.toString();
    }

    private DetailedError getDetailedError(String data, Throwable cause) {
        DetailedError detailedError = new DetailedError(cause);
        if (data != null && data.length() > 0) {
            try {
                objectMapper.readerForUpdating(detailedError).readValue(data);
            } catch (IOException e) {
                detailedError.setMessage(data);
            }
        }
        return detailedError;
    }

    protected RequestBuilder createRequest(Method method, Object[] arguments) {
        JaxRsMeta meta = new JaxRsMeta(method);

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
    }

    protected Observable<Object> deserialize(Method method, String string) {
        if (string == null || string.isEmpty()) {
            return Observable.empty();
        }
        Type type = ReflectionUtil.getTypeOfObservable(method);
        try {
            JavaType     javaType = TypeFactory.defaultInstance().constructType(type);
            ObjectReader reader   = objectMapper.readerFor(javaType);
            return just(reader.readValue(string));
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
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < types.length; i++) {
            Object value = arguments[i];
            for (Annotation annotation : annotations[i]) {
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
                }
            }
        }
        if (query != null) {
            return path + query;
        }
        return path;
    }

    protected String serialize(Object value) {
        if (value instanceof Date) {
            return String.valueOf(((Date)value).getTime());
        }
        if (value.getClass().isArray()) {
            value = Arrays.asList((Object[])value);
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

    public static class DetailedError extends Throwable {
        private int    code;
        private String error;
        private String message;

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
    }

    public static class ThrowableWithoutStack extends Throwable {
        public ThrowableWithoutStack(String message) {
            super(message, null, false, false);
        }
    }
}
