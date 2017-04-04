package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.MockHttpServerRequest;
import se.fortnox.reactivewizard.MockHttpServerResponse;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JaxRsTestUtil {

	public static MockHttpServerResponse get(Object service, String uri) {
		return get(service, uri, null);
	}

	public static MockHttpServerResponse get(Object service, String uri, Map<String, List<String>> query) {
		MockHttpServerRequest request = new MockHttpServerRequest(uri, query);
		return processRequest(service, request);
	}

	public static JaxRsResources resources(Object service) {
		return new JaxRsResources(
				new Object[] { service },
				new JaxRsResourceFactory(),
				false);
	}

	public static MockHttpServerResponse getWithHeaders(Object service, String uri, Map<String, List<String>> headers) {
		MockHttpServerRequest request = new MockHttpServerRequest(uri);
		for (Map.Entry<String, List<String>> header : headers.entrySet()) {
			request.addHeader(header.getKey(), header.getValue().get(0));
		}
		return processRequest(service, request);
	}

	public static MockHttpServerResponse post(Object service, String uri, String data) {
		return sendWithMethod(HttpMethod.POST, service, uri, data);
	}

	public static MockHttpServerResponse post(Object service, String uri, byte[] data) {
		HttpServerRequest<ByteBuf> request = new MockHttpServerRequest(uri, HttpMethod.POST, data);
		return processRequest(service, request);
	}

	public static MockHttpServerResponse put(Object service, String uri, String data) {
		return sendWithMethod(HttpMethod.PUT, service, uri, data);
	}

	public static MockHttpServerResponse patch(Object service, String uri, String data) {
		return sendWithMethod(HttpMethod.PATCH, service, uri, data);
	}

	public static MockHttpServerResponse delete(Object service, String uri, String data) {
		return sendWithMethod(HttpMethod.DELETE, service, uri, data);
	}

	private static MockHttpServerResponse sendWithMethod(HttpMethod method, Object service, String uri, String data) {
		HttpServerRequest<ByteBuf> request = new MockHttpServerRequest(uri, method, data);
		return processRequest(service, request);
	}

	public static MockHttpServerResponse processRequest(Object service, HttpServerRequest<ByteBuf> request) {
		JaxRsRequestHandler handler = getJaxRsRequestHandler(service);

		return processRequestWithHandler(handler, request);
	}

	private static JaxRsRequestHandler getJaxRsRequestHandler(Object service) {
		return new JaxRsRequestHandler(new Object[] { service },
                    new JaxRsResourceFactory(new ParamResolverFactories(), new JaxRsResultFactoryFactory(), new BlockingResourceScheduler()),
                    new ExceptionHandler(),
                    false);
	}

	public static MockHttpServerResponse processRequestWithHandler(JaxRsRequestHandler handler, HttpServerRequest<ByteBuf> request) {
		ExceptionHandler exceptionHandler = new ExceptionHandler();
		MockHttpServerResponse response = new MockHttpServerResponse();
		Observable<Void> result = handler.handle(request, response);
		if (result == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
		} else {
			result.onErrorReturn(e -> {
				exceptionHandler.handleException(request, response, e);
				return null;
			}).toBlocking().singleOrDefault(null);
		}
		return response;
	}

	public static String body(MockHttpServerResponse response) {
		return response.getOutp();
	}

	public static Map<String, List<String>> qp(String key, String val) {
		Map<String, List<String>> params = new HashMap<>();
		params.put(key, Arrays.asList(val));
		return params;
	}

	public static TestServer testServer(Object service) {
		return new TestServer(getJaxRsRequestHandler(service));
	}

	static class TestServer {

		private final HttpServer<ByteBuf, ByteBuf> server;
		private final String uriPrefix;

		public TestServer(RequestHandler requestHandler) {
			server = RxNetty.createHttpServer(0, requestHandler);
			server.start();
			uriPrefix = "http://localhost:"+server.getServerPort();
		}

		public String get(String uri) {
			return RxNetty.createHttpGet(uriPrefix+uri)
					.flatMap(resp->resp.getContent())
					.map(buf->buf.toString(Charset.defaultCharset()))
					.toBlocking()
					.single();
		}

	}

}
