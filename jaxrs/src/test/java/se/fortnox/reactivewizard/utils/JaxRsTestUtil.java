package se.fortnox.reactivewizard.utils;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceFactory;
import se.fortnox.reactivewizard.jaxrs.JaxRsResources;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class JaxRsTestUtil {

    public static MockHttpServerResponse get(Object service, String uri) {
        MockHttpServerRequest request = new MockHttpServerRequest(uri);
        return processRequest(service, request);
    }

    public static JaxRsResources resources(Object service) {
        return new JaxRsResources(
            new Object[]{service},
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

    public static JaxRsRequestHandler getJaxRsRequestHandler(Object... services) {
        return new JaxRsRequestHandler(services);
    }

    public static MockHttpServerResponse processRequestWithHandler(JaxRsRequestHandler handler, HttpServerRequest<ByteBuf> request) {
        ExceptionHandler       exceptionHandler = new ExceptionHandler();
        MockHttpServerResponse response         = new MockHttpServerResponse();
        Observable<Void>       result           = handler.handle(request, response);
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

    public static TestServer testServer(Object... services) {
        return new TestServer(getJaxRsRequestHandler(services));
    }

	public static class TestServer {

        private final HttpServer<ByteBuf, ByteBuf> server;

        public TestServer(RequestHandler requestHandler) {
            server = HttpServer.newServer(0).start(requestHandler);
        }

        public HttpServer<ByteBuf, ByteBuf> getServer() {
            return server;
        }

        public String get(String uri) {
            return HttpClient.newClient("localhost", server.getServerPort())
                .createGet(uri)
                .flatMap(HttpClientResponse::getContent)
                .map(buf -> buf.toString(Charset.defaultCharset()))
                .toBlocking()
                .single();
        }

    }

}
