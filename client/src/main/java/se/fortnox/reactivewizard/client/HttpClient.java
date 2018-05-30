package se.fortnox.reactivewizard.client;

import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static rx.Observable.just;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import se.fortnox.reactivewizard.jaxrs.ByteBufCollector;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.logging.LoggingContext;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.ManifestUtil;
import se.fortnox.reactivewizard.util.ReflectionUtil;
import se.fortnox.reactivewizard.util.rx.RetryWithDelay;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Single;

public class HttpClient implements InvocationHandler {

	private static final Logger			LOG			= LoggerFactory.getLogger(HttpClient.class);

	protected final InetSocketAddress serverInfo;

	private static String hostName;
	static {
		try {
			// A number of HttpClient instances is created (at least during tests)
			// and the following call takes a while so the result should be reused
			// to avoid slowing down the application's start
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new RuntimeException("Cannot get the localhost address.", e);
		}
	}

	private final RequestParameterSerializers requestParameterSerializers;

	protected final HttpClientConfig	config;

	private RxClientProvider			clientProvider;
	private ObjectMapper objectMapper;

	private int      timeout = 10;
	private TimeUnit timeoutUnit = TimeUnit.SECONDS;

	private final RequestTracer requestTracer;

	private static final ByteBufCollector COLLECTOR = new ByteBufCollector(10*1024*1024);

	@Inject
	public HttpClient(HttpClientConfig config, RxClientProvider clientProvider, ObjectMapper objectMapper, RequestParameterSerializers requestParameterSerializers) {
		this.config = config;
		this.clientProvider = clientProvider;
		this.objectMapper = objectMapper;
		this.requestParameterSerializers = requestParameterSerializers;

		serverInfo = new InetSocketAddress(config.getHost(), config.getPort());

		this.requestTracer = ManifestUtil.getManifestValues()
			.map(manifestValues -> new RequestTracer(manifestValues, hostName))
			.orElse(null);
	}

	public HttpClient(HttpClientConfig config) {
		this(config, new RxClientProvider(config, new HealthRecorder()), new ObjectMapper(), new RequestParameterSerializers());
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
				((HttpClient) handler).setTimeout(timeout, timeoutUnit);
			}
		}
	}

	public void setTimeout(int timeout, TimeUnit timeoutUnit) {
		this.timeout = timeout;
		this.timeoutUnit = timeoutUnit;
	}

	@SuppressWarnings("unchecked")
	public <T> T create(Class<T> jaxRsInterface) {
		return (T) Proxy.newProxyInstance(jaxRsInterface.getClassLoader(), new Class[] { jaxRsInterface }, this);
	}

	public static <T> T create(HttpClientConfig config,
			RxClientProvider clientProvider, ObjectMapper objectMapper, Class<T> jaxRsInterface) {
		return new HttpClient(config, clientProvider, objectMapper, new RequestParameterSerializers()).create(jaxRsInterface);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (args == null) {
			args = new Object[0];
		}

		RequestBuilder fullReq = createRequest(method, args);

		addDevOverrides(fullReq);

		final io.reactivex.netty.protocol.http.client.HttpClient<ByteBuf, ByteBuf> rxClient = clientProvider.clientFor(fullReq.getServerInfo());

		if (LOG.isDebugEnabled()) {
			LOG.debug(fullReq + " with headers " + fullReq.getHeaders().entrySet());
		}

		final Observable<HttpClientResponse<ByteBuf>> resp = fullReq.submit(rxClient).timeout(timeout, timeoutUnit);

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

	private void logFailedRequest(RequestBuilder fullReq, Throwable e) {
		if (e instanceof WebException && ((WebException) e).getStatus().code() < 500) {
			// Don't log 400 bad request and similar
			return;
		}
		LOG.warn("Failed " + fullReq, e);
	}

	protected Observable<?> parseResponse(Method method, RequestBuilder request, HttpClientResponse<ByteBuf> response) {
		final Observable<String> body = COLLECTOR.collectString(response.getContent()).singleOrDefault("");

		if (expectsByteArrayResponse(method)) {
			if (response.getStatus().code() >= 400) {
				return body.map(data -> handleError(request, response, data));
			}

			return COLLECTOR.collectBytes(response.getContent());
		}

		return body
				.map(data -> handleError(request, response, data))
				.flatMap(str -> deserialize(method, str));
	}

	private static final Class BYTEARRAY_TYPE = (new byte[0]).getClass();

	private boolean expectsByteArrayResponse(Method method) {
		Type t = ReflectionUtil.getTypeOfObservable(method);
		return t.equals(BYTEARRAY_TYPE);
	}

	private void addDevOverrides(RequestBuilder fullReq) {
		if (config.getDevServerInfo() != null) {
			fullReq.setServerInfo(config.getDevServerInfo());
		}

		if (config.getDevCookie() != null) {
			String cookie = fullReq.getHeaders().get("Cookie") + ";" + config.getDevCookie();
			fullReq.getHeaders().remove("Cookie");
			fullReq.addHeader("Cookie", cookie);
		}

		if (config.getDevHeaders() != null && config.getDevHeaders() != null) {
			config.getDevHeaders().forEach(fullReq::addHeader);
		}
	}

	protected <T> Observable<T> measure(RequestBuilder fullReq, Observable<T> output) {
		return Metrics.get("OUT_res:"+fullReq.getKey()).measure(output);
	}

	protected <T> Observable<T> withRetry(RequestBuilder fullReq, Observable<T> resp) {
		return resp.retryWhen(new RetryWithDelay(config.getRetryCount(), config.getRetryDelayMs(),
				throwable -> {
					if (throwable instanceof TimeoutException) {
						return false;
					}
					if (!(throwable instanceof WebException)) {
						// Retry on system error of some kind, like IO
						return true;
					}
					if (fullReq.getHttpMethod().equals(HttpMethod.POST)) {
						// Don't retry if it was a POST, as it is not idempotent
						return false;
					}
					if (((WebException) throwable).getStatus().code() >= 500) {
						// Retry if it's 500+ error
						return true;
					}
					// Don't retry if it is a 400 error or something like that
					return false;
				}));
	}

	private boolean expectsRawResponse(Method method) {
		Type t = ReflectionUtil.getTypeOfObservable(method);
		return (t instanceof ParameterizedType && ((ParameterizedType) t).getRawType().equals(HttpClientResponse.class));
	}

	protected void addContent(Method method, Object[] args, RequestBuilder req) {
		if (!req.canHaveBody()) {
			return;
		}
		Class<?>[] types = method.getParameterTypes();
		Annotation[][] annotations = method.getParameterAnnotations();
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < types.length; i++) {
			Object val = args[i];
			if (val == null) {
				continue;
			}
			FormParam formParam = getFormParam(annotations[i]);
			if (formParam != null) {
				addFormParamToOutput(output, val, formParam);
				req.getHeaders().put(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
			} else if (isBodyArg(types[i], annotations[i])) {
				try {
					if (!req.getHeaders().containsKey(CONTENT_TYPE)) {
						req.getHeaders().put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
					}
					req.setContent(objectMapper.writeValueAsBytes(val));
					return;
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		}
		if (output.length() > 0) {
			req.setContent(output.toString());
		}
	}

	protected void addFormParamToOutput(StringBuilder output, Object val, FormParam formParam) {
		if (output.length() != 0) {
			output.append("&");
		}
		output.append(formParam.value()).append("=").append(urlEncode(val.toString()));
	}

	private FormParam getFormParam(Annotation[] annotations) {
		for (Annotation annotation : annotations) {
			if (annotation instanceof FormParam) {
				return (FormParam) annotation;
			}
		}
		return null;
	}

	protected boolean isBodyArg(@SuppressWarnings("unused") Class<?> cls, Annotation[] annotations) {
		for (Annotation pa : annotations) {
			if (pa instanceof QueryParam || pa instanceof PathParam || pa instanceof HeaderParam || pa instanceof CookieParam) {
				return false;
			}
		}
		return true;
	}

	protected String handleError(RequestBuilder request, HttpClientResponse<ByteBuf> r, String data) {
		if (r.getStatus().code() >= 400) {
			Throwable detailedErrorCause = new ThrowableWithoutStack(format("Error calling other service:\n\tResponse Status: %d\n\tURL: %s\n\tRequest Headers: %s\n\tResponse Headers: %s\n\tData: %s",
					r.getStatus().code(),
					request.getFullUrl() ,
					request.getHeaders().entrySet(),
					formatHeaders(r),
					data));
			DetailedError detailedError = getDetailedError(data, detailedErrorCause);
			String reasonPhrase = detailedError.hasReason() ? detailedError.reason() : r.getStatus().reasonPhrase();
			HttpResponseStatus responseStatus = new HttpResponseStatus(r.getStatus()
					.code(),
					reasonPhrase);
			WebException we = new WebException(responseStatus, detailedError, false);
			throw we;
		}
		return data;
	}

	private String formatHeaders(HttpClientResponse<ByteBuf> r) {
		StringBuilder headers = new StringBuilder();
		r.headerIterator().forEachRemaining(h->headers.append(h.getKey()).append('=').append(h.getValue()).append(' '));
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

	protected RequestBuilder createRequest(Method method, Object[] args) {
		JaxRsMeta meta = new JaxRsMeta(method);

		RequestBuilder request = new RequestBuilder(serverInfo, meta.getHttpMethod(), meta.getFullPath());
		request.setUri(getPath(method, args, meta));
		setHeaderParams(request, method, args);
		addCustomParams(request, method, args);

		Consumes consumes = method.getAnnotation(Consumes.class);
		if (consumes != null && consumes.value().length != 0) {
			request.addHeader("Content-Type", consumes.value()[0]);
		}

		if (requestTracer != null) {
			requestTracer.addTrace(request);
		}

		addContent(method, args, request);

		return request;
	}

	@SuppressWarnings("unchecked")
	private void addCustomParams(RequestBuilder request, Method method, Object[] args) {
		Class<?>[] types = method.getParameterTypes();
		for (int i = 0; i < types.length; i++) {
			RequestParameterSerializer serializer = requestParameterSerializers.getSerializer(types[i]);
			if (serializer != null) {
				serializer.addParameter(args[i], request);
			}
		}
	}

	private void setHeaderParams(RequestBuilder request, Method method, Object[] args) {
		Class<?>[] types = method.getParameterTypes();
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < types.length; i++) {
			Object val = args[i];
			if (val == null) {
				continue;
			}
			for (Annotation a : annotations[i]) {
				if (a instanceof HeaderParam) {
					request.addHeader(((HeaderParam) a).value(), serialize(val));
				} else if (a instanceof CookieParam) {
					final String currentCookieValue = request.getHeaders().get("Cookie");
					final String cookiePart = ((CookieParam) a).value() + "=" + serialize(val);
					if(currentCookieValue != null) {
						request.addHeader("Cookie", format("%s; %s", currentCookieValue, cookiePart));
					} else {
						request.addHeader("Cookie", cookiePart);
					}
				}
			}
		}
	}

	protected Observable<Object> deserialize(Method method, String str) {
		if (str == null || str.isEmpty()) {
			return Observable.empty();
		}
		Type t = ReflectionUtil.getTypeOfObservable(method);
		try {
			JavaType type = TypeFactory.defaultInstance().constructType(t);
			ObjectReader reader = objectMapper.readerFor(type);
			return just(reader.readValue(str));
		} catch(IOException e) {
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

	protected String getPath(Method method, Object[] args, JaxRsMeta meta) {
		String path = meta.getFullPath();

		StringBuilder query = null;
		Class<?>[] types = method.getParameterTypes();
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < types.length; i++) {
			Object val = args[i];
			for (Annotation a : annotations[i]) {
				if (a instanceof QueryParam) {
					if (val == null) {
						continue;
					}
					if (query == null) {
						query = new StringBuilder(path.contains("?") ? "&" : "?");
					} else {
						query.append('&');
					}
					query.append(((QueryParam) a).value());
					query.append('=');
					query.append(encode(serialize(val)));
				} else if (a instanceof PathParam) {
					if (path.contains("{" + ((PathParam)a).value() + ":.*}")) {
                        path = path.replaceAll("\\{" + ((PathParam)a).value() + ":.*\\}", this.encode(this.serialize(val)));
					} else {
						path = path.replaceAll("\\{" + ((PathParam)a).value() + "\\}", this.urlEncode(this.serialize(val)));
					}
				}
			}
		}
		if (query != null) {
			return path + query;
		}
		return path;
	}

	protected String serialize(Object val) {
		if (val instanceof Date) {
			return String.valueOf(((Date) val).getTime());
		}
		if (val.getClass().isArray()) {
			val = Arrays.asList((Object[]) val);
		}
		if (val instanceof List) {
			StringBuilder sb = new StringBuilder();
			List values = (List) val;
			for (int i = 0; i < values.size(); i++) {
				Object value = values.get(i);
				sb.append(value);
				if (i < values.size() - 1) {
					sb.append(",");
				}
			}
			return sb.toString();
		}
		return val.toString();
	}

	public static class DetailedError extends Throwable {
		private int code;
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
