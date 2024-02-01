package se.fortnox.reactivewizard.utils;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceFactory;
import se.fortnox.reactivewizard.jaxrs.JaxRsResources;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolvers;
import se.fortnox.reactivewizard.jaxrs.params.ParamTypeResolver;
import se.fortnox.reactivewizard.jaxrs.params.annotated.AnnotatedParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;
import se.fortnox.reactivewizard.jaxrs.response.ResultTransformerFactories;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import se.fortnox.reactivewizard.mocks.MockHttpServerRequest;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static reactor.core.publisher.Flux.empty;

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
            request.requestHeaders().add(header.getKey(), header.getValue().getFirst());
        }
        return processRequest(service, request);
    }

    public static MockHttpServerResponse post(Object service, String uri, String data) {
        return sendWithMethod(HttpMethod.POST, service, uri, data);
    }

    public static MockHttpServerResponse post(Object service, String uri, byte[] data) {
        MockHttpServerRequest request = new MockHttpServerRequest(uri, HttpMethod.POST, data);
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
        MockHttpServerRequest request = new MockHttpServerRequest(uri, method, data);
        return processRequest(service, request);
    }

    public static MockHttpServerResponse processRequest(Object service, HttpServerRequest request) {
        JaxRsRequestHandler handler = getJaxRsRequestHandler(service);

        return processRequestWithHandler(handler, request);
    }

    private static JaxRsRequestHandler getJaxRsRequestHandler(Object... services) {
        return new JaxRsRequestHandler(services,
            new JaxRsResourceFactory(
                new ParamResolverFactories(new DeserializerFactory(), new ParamResolvers(),
                    new AnnotatedParamResolverFactories(new DeserializerFactory()), new ParamTypeResolver()),
                new JaxRsResultFactoryFactory(),
                new RequestLogger()), new ExceptionHandler(), true);
    }

    public static MockHttpServerResponse processRequestWithHandler(JaxRsRequestHandler handler, HttpServerRequest request) {
        ExceptionHandler       exceptionHandler = new ExceptionHandler();
        MockHttpServerResponse response         = new MockHttpServerResponse();
        Publisher<Void>        result           = handler.apply(request, response);
        if (result == null) {
            response.status(HttpResponseStatus.NOT_FOUND);
        } else {
            Flux.from(result).onErrorResume(e -> {
                exceptionHandler.handleException(request, response, e);
                return empty();
            }).count().block();
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

        private final DisposableServer server;

        public TestServer(RequestHandler requestHandler) {
            server = HttpServer.create().handle(requestHandler).bindNow();
        }

        public DisposableServer getServer() {
            return server;
        }

        public String get(String uri) {
            return HttpClient.create().baseUrl("http://localhost:"+server.port()+uri)
                .get()
                .responseContent()
                .map(buf -> buf.toString(Charset.defaultCharset()))
                .blockLast();
        }

    }

}
