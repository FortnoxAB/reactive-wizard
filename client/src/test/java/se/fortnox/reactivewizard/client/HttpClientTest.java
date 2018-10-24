package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.internal.SingleHostConnectionProvider;
import io.reactivex.netty.client.pool.PoolConfig;
import io.reactivex.netty.client.pool.PoolExhaustedException;
import io.reactivex.netty.client.pool.PooledConnectionProvider;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Single;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.jaxrs.ByteBufCollector;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.PATCH;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import se.fortnox.reactivewizard.server.ServerConfig;
import se.fortnox.reactivewizard.test.TestUtil;
import se.fortnox.reactivewizard.util.rx.RetryWithDelay;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static java.lang.String.format;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static rx.Observable.defer;
import static rx.Observable.empty;
import static rx.Observable.just;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

public class HttpClientTest {

    private HealthRecorder healthRecorder = new HealthRecorder();

    @Test
    public void shouldNotRetryFailedPostCalls() {
        AtomicLong                   callCount = new AtomicLong();
        HttpServer<ByteBuf, ByteBuf> server    = startServer(INTERNAL_SERVER_ERROR, "\"NOT OK\"", r -> callCount.incrementAndGet());

        long start = System.currentTimeMillis();
        try {
            TestResource resource = getHttpProxy(server.getServerPort());

            resource.postHello().toBlocking().lastOrDefault(null);

            Assert.fail("Expected exception");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;

            assertThat(duration).isLessThan(1000);
            assertThat(callCount.get()).isEqualTo(1);

            assertThat(e.getClass()).isEqualTo(WebException.class);
        } finally {
            server.shutdown();
        }
    }

    protected TestResource getHttpProxy(int port) {
        return getHttpProxy(port, 1, 10000);
    }

    protected TestResource getHttpProxy(int port, int maxConn) {
        return getHttpProxy(port, maxConn, 10000);
    }

    protected TestResource getHttpProxy(int port, int maxConn, int maxRequestTime) {
        try {
            HttpClientConfig config = new HttpClientConfig("localhost:" + port);
            config.setMaxConnections(maxConn);
            config.setReadTimeoutMs(maxRequestTime);
            return getHttpProxy(config);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private TestResource getHttpProxy(HttpClientConfig config) {
        HttpClient client = new HttpClient(config, new RxClientProvider(config, healthRecorder), new ObjectMapper(), new RequestParameterSerializers(), Collections.emptySet());
        return client.create(TestResource.class);
    }

    protected void withServer(Consumer<HttpServer<ByteBuf, ByteBuf>> serverConsumer) {
        final HttpServer<ByteBuf, ByteBuf> server = startServer(HttpResponseStatus.OK, "\"OK\"");

        LogManager.getLogger(RetryWithDelay.class).setLevel(Level.toLevel(Level.OFF_INT));

        try {
            serverConsumer.accept(server);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void shouldRetryOnFullConnectionPool() {
        withServer(server -> {
            long start = System.currentTimeMillis();
            try {
                HttpClientConfig config = new HttpClientConfig("127.0.0.1:" + server.getServerPort());
                config.setRetryCount(1);
                config.setRetryDelayMs(1000);
                config.setMaxConnections(1);
                TestResource resource = getHttpProxy(config);

                List<Observable<String>> results = new ArrayList<Observable<String>>();
                for (int i = 0; i < 10; i++) {
                    results.add(resource.getHello());
                }

                Observable.merge(results)
                    .takeLast(0)
                    .toBlocking()
                    .lastOrDefault(null);

                Assert.fail("Expected exception");
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;

                assertThat(e.getCause().getClass()).isEqualTo(PoolExhaustedException.class);

                assertThat(duration).isGreaterThan(1000);
            }
        });
    }

    @Test
    public void shouldReportUnhealthyWhenPoolIsExhausted() {

        AtomicBoolean wasUnhealthy = new AtomicBoolean(false);

        healthRecorder = new HealthRecorder() {
            @Override
            public boolean logStatus(Object key, boolean currentStatus) {
                if (!currentStatus) {
                    wasUnhealthy.set(true);
                }
                return super.logStatus(key, currentStatus);
            }
        };

        withServer(server -> {
            try {
                HttpClientConfig config = new HttpClientConfig("127.0.0.1:" + server.getServerPort());
                config.setMaxConnections(1);
                config.setRetryCount(0);
                TestResource resource = getHttpProxy(config);

                List<Observable<String>> results = new ArrayList<Observable<String>>();
                for (int i = 0; i < 10; i++) {
                    results.add(resource.getHello());
                }

                Observable.merge(results)
                    .takeLast(0)
                    .toBlocking()
                    .lastOrDefault(null);

                Assert.fail("Expected exception");
            } catch (Exception e) {

                assertThat(e.getCause().getClass()).isEqualTo(PoolExhaustedException.class);

                assertThat(wasUnhealthy.get()).isTrue();
            }
        });
    }

    @Test
    public void shouldReportHealthyWhenPoolIsNotExhausted() {
        withServer(server -> {
            TestResource resource = getHttpProxy(server.getServerPort(), 5);

            List<Observable<String>> results = new ArrayList<Observable<String>>();
            for (int i = 0; i < 5; i++) {
                results.add(resource.getHello());
            }

            Observable.merge(results)
                .takeLast(0)
                .toBlocking()
                .lastOrDefault(null);

            assertThat(healthRecorder.isHealthy()).isTrue();
        });
    }

    @Test
    public void shouldRetryOnConnectionRefused() {
        long start = System.currentTimeMillis();
        try {
            // A port that is not in use and will give connection refused
            HttpClientConfig config = new HttpClientConfig("127.0.0.1:8282");
            config.setRetryDelayMs(100);
            config.setRetryCount(1);
            TestResource resource = getHttpProxy(config);

            resource.getHello().toBlocking().lastOrDefault(null);

            Assert.fail("Expected exception");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;

            assertThat(e.getCause()).isInstanceOf(ConnectException.class);

            assertThat(duration).isGreaterThan(100);
        }
    }

    @Test
    public void shouldReturnDetailedError() {
        String                       detailedErrorJson = "{\"error\":1,\"message\":\"Detailed error description.\",\"code\":100}";
        HttpServer<ByteBuf, ByteBuf> server            = startServer(HttpResponseStatus.BAD_REQUEST, detailedErrorJson);

        TestResource resource = getHttpProxy(server.getServerPort());
        try {
            resource.getHello().toBlocking().single();
            Assert.fail("Expected exception");
        } catch (WebException e) {
            assertThat(e.getCause()).isInstanceOf(HttpClient.DetailedError.class);
            HttpClient.DetailedError detailedError = (HttpClient.DetailedError)e.getCause();
            assertThat(detailedError.getError()).isEqualTo("1");
            assertThat(detailedError.getCode()).isEqualTo(100);
            assertThat(detailedError.getMessage()).isEqualTo("Detailed error description.");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void shouldSerializeDate() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        assertThat(client.serialize(new Date(1474889891615L))).isEqualTo("1474889891615");
    }

    @Test
    public void shouldSerializeList() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        assertThat(client.serialize(new Long[]{})).isEqualTo("");
        assertThat(client.serialize(new Long[]{5L, 78L, 1005L})).isEqualTo("5,78,1005");
        assertThat(client.serialize(new ArrayList<>())).isEqualTo("");
        assertThat(client.serialize(Arrays.asList(5L, 78L, 1005L))).isEqualTo("5,78,1005");
    }

    @Test
    public void shouldEncodePath() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        Method     method = TestResource.class.getMethod("withPathAndQueryParam", String.class, String.class);
        String     path   = client.getPath(method, new Object[]{"key_with_ä", "value_with_+"}, new JaxRsMeta(method, null));
        assertThat(path).isEqualTo("/hello/{fid}/key_with_%C3%A4?value=value_with_%2B");
    }

    @Test
    public void shouldEncodeWithMultipleQueryParams() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        Method     method = TestResource.class.getMethod("withMultipleQueryParam", String.class, String.class);
        String     path   = client.getPath(method, new Object[]{"first", "second"}, new JaxRsMeta(method, null));
        assertThat(path).isEqualTo("/hello/multipleQueryParams?valueA=first&valueB=second");
    }

    @Test
    public void shouldEncodePathWithoutNullValues() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        Method     method = TestResource.class.getMethod("withPathAndQueryParam", String.class, String.class);
        String     path   = client.getPath(method, new Object[]{"path", null}, new JaxRsMeta(method, null));
        assertThat(path).isEqualTo("/hello/{fid}/path");
    }

    @Test
    public void shouldNotEncodePathWithSlash() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        Method     method = TestResource.class.getMethod("withRegExpPathAndQueryParam", String.class, String.class);
        String     path   = client.getPath(method, new Object[]{"key/with/Slash", "value/with/slash"}, new JaxRsMeta(method, null));
        assertThat(path).isEqualTo("/hello/{fid}/key/with/Slash?value=value%2Fwith%2Fslash");
    }

    @Test
    public void shouldEncodePathWithSlash() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        Method     method = TestResource.class.getMethod("withPathAndQueryParam", String.class, String.class);
        String     path   = client.getPath(method, new Object[]{"key/with/Slash", "value/with/slash"}, new JaxRsMeta(method, null));
        assertThat(path).isEqualTo("/hello/{fid}/key%2Fwith%2FSlash?value=value%2Fwith%2Fslash");
    }

    @Test
    public void shouldEncodePathAndQueryWithColon() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        Method     method = TestResource.class.getMethod("withPathAndQueryParam", String.class, String.class);
        String     path   = client.getPath(method, new Object[]{"path:param", "query-param-with:colon"}, new JaxRsMeta(method, null));
        assertThat(path).isEqualTo("/hello/{fid}/path%3Aparam?value=query-param-with%3Acolon");
    }

    @Test
    public void shouldSupportSingleSource() {
        HttpServer<ByteBuf, ByteBuf> server = startServer(HttpResponseStatus.OK, "\"OK\"");

        TestResource resource = getHttpProxy(server.getServerPort());
        resource.getSingle().toBlocking().value();

        server.shutdown();
    }

    @Test
    public void shouldHandleLargeResponses() {
        final HttpServer<ByteBuf, ByteBuf> server   = startServer(HttpResponseStatus.OK, generate10MbString());
        TestResource                       resource = getHttpProxy(server.getServerPort());
        resource.getHello().toBlocking().single();

        server.shutdown();
    }

    @Test
    public void shouldSupportByteArrayResponse() {
        final HttpServer<ByteBuf, ByteBuf> server   = startServer(HttpResponseStatus.OK, "hej");
        TestResource                       resource = getHttpProxy(server.getServerPort());
        byte[]                             result   = resource.getAsBytes().toBlocking().single();
        assertThat(new String(result)).isEqualTo("hej");

        server.shutdown();
    }

    @Test
    public void shouldRetry5XXesponses() throws URISyntaxException {
        AtomicLong                   callCount = new AtomicLong();
        HttpServer<ByteBuf, ByteBuf> server    = startServer(INTERNAL_SERVER_ERROR, "", r -> callCount.incrementAndGet());

        HttpClientConfig config = new HttpClientConfig("127.0.0.1:" + server.getServerPort());
        config.setRetryDelayMs(10);
        TestResource resource = getHttpProxy(config);
        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
        }
        assertThat(callCount.get()).isEqualTo(4);

        server.shutdown();
    }

    @Test
    public void shouldNotRetryFor4XXResponses() {
        AtomicLong                   callCount = new AtomicLong();
        HttpServer<ByteBuf, ByteBuf> server    = startServer(NOT_FOUND, "", r -> callCount.incrementAndGet());

        TestResource resource = getHttpProxy(server.getServerPort());
        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(HttpResponseStatus.NOT_FOUND);
        }
        assertThat(callCount.get()).isEqualTo(1);

        server.shutdown();
    }

    @Test
    public void shouldNotRetryForJsonParseErrors() {
        AtomicLong                   callCount = new AtomicLong();
        HttpServer<ByteBuf, ByteBuf> server    = startServer(OK, "{\"result\":[]}", r -> callCount.incrementAndGet());

        TestResource resource = getHttpProxy(server.getServerPort());
        try {
            resource.getWrappedPojo().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(JsonMappingException.class);
        }
        assertThat(callCount.get()).isEqualTo(1);

        server.shutdown();
    }

    @Test
    public void shouldNotRetryOnTimeout() {
        AtomicLong callCount = new AtomicLong();
        // Slow server
        HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(0)
            .start((request, response) -> {
                callCount.incrementAndGet();
                return Observable.defer(() -> {
                    response.setStatus(HttpResponseStatus.NOT_FOUND);
                    return Observable.<Void>empty();
                }).delaySubscription(1000, TimeUnit.MILLISECONDS);
            });

        TestResource resource = getHttpProxy(server.getServerPort());
        HttpClient.setTimeout(resource, 500, TimeUnit.MILLISECONDS);
        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(TimeoutException.class);
        }
        assertThat(callCount.get()).isEqualTo(1);

        server.shutdown();
    }

    @Test
    public void shouldHandleLongerRequestsThan10SecondsWhenRequested() {
        // Slow server
        HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(0)
            .start((request, response) -> {

                return Observable.defer(() -> {
                    response.setStatus(HttpResponseStatus.NOT_FOUND);
                    return Observable.<Void>empty();
                }).delaySubscription(20000, TimeUnit.MILLISECONDS);
            });

        TestResource resource = getHttpProxy(server.getServerPort(), 1, 30000);
        HttpClient.setTimeout(resource, 15000, TimeUnit.MILLISECONDS);
        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(TimeoutException.class);
        }

        server.shutdown();
    }

    @Test
    public void shouldThrowNettyReadTimeoutIfRequestTakesLongerThanClientIsConfigured() {
        // Slow server
        HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(0)
            .start((request, response) -> Observable.defer(() -> {
                response.setStatus(HttpResponseStatus.NOT_FOUND);
                return Observable.<Void>empty();
            }).delaySubscription(1000, TimeUnit.MILLISECONDS));

        //Create a resource with 500ms limit
        TestResource resource = getHttpProxy(server.getServerPort(), 1, 500);

        //Lets set the observable timeout higher than the httpproxy readTimeout
        HttpClient.setTimeout(resource, 1000, TimeUnit.MILLISECONDS);

        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e).isInstanceOf(io.netty.handler.timeout.TimeoutException.class);
        }

        server.shutdown();
    }

    @Test
    public void shouldHandleMultipleChunks() {
        HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(0).start((request, response) -> {
            response.setStatus(HttpResponseStatus.OK);
            return response.writeStringAndFlushOnEach(just("\"he")
                .concatWith(defer(() -> just("llo\"")))
                .concatWith(defer(Observable::empty)));
        });

        TestResource resource = getHttpProxy(server.getServerPort());
        String       result   = resource.getHello().toBlocking().single();
        assertThat(result).isEqualTo("hello");

        server.shutdown();
    }

    @Test
    public void shouldDeserializeVoidResult() {
        HttpServer<ByteBuf, ByteBuf> server = startServer(HttpResponseStatus.CREATED, "");

        TestResource resource = getHttpProxy(server.getServerPort());
        resource.getVoid().toBlocking().singleOrDefault(null);

        server.shutdown();
    }

    @Test
    public void shouldShutDownConnectionOnTimeoutBeforeHeaders() throws URISyntaxException {
        Consumer<String>             serverLog = mock(Consumer.class);
        HttpServer<ByteBuf, ByteBuf> server    = createTestServer(serverLog);

        HttpClientConfig config = new HttpClientConfig("localhost:" + server.getServerPort());
        config.setMaxConnections(1);
        config.setRetryCount(0);
        HttpClient   client   = new HttpClient(config);
        TestResource resource = client.create(TestResource.class);
        HttpClient.setTimeout(resource, 200, TimeUnit.MILLISECONDS);

        resource.servertest("fast")
            // Delay needed so that repeat will not subscribe immediately, as the pooled connection is released on the event loop thread.
            .delaySubscription(10, TimeUnit.MILLISECONDS)
            .repeat(10)
            .doOnNext(System.out::println)
            .toBlocking()
            .last();

        verify(serverLog, times(10)).accept("/hello/servertest/fast");

        try {
            resource.servertest("slowHeaders")
                .delaySubscription(10, TimeUnit.MILLISECONDS)
                .retry(5)
                .doOnNext(System.out::println)
                .toBlocking()
                .last();
            fail("Expected exception, but none was thrown");
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(TimeoutException.class);
        }

        verify(serverLog, times(6)).accept("/hello/servertest/slowHeaders");

        server.shutdown();
    }

    @Test
    public void shouldShutDownConnectionOnTimeoutEstablishingConnection() throws URISyntaxException {
        HttpClientConfig config = new HttpClientConfig("http://127.0.0.1");
        config.setMaxConnections(1);

        AtomicLong connectionsRequested = new AtomicLong();

        ConnectionProvider<ByteBuf, ByteBuf> connectionProvider = () -> Observable.<Connection<ByteBuf, ByteBuf>>never()
            .doOnSubscribe(connectionsRequested::incrementAndGet);

        // Setup a clientProvider that never returns any connections
        RxClientProvider clientProvider = new RxClientProvider(config, new HealthRecorder()) {
            @Override
            protected ConnectionProviderFactory<ByteBuf, ByteBuf> createConnectionProviderFactory(PoolConfig<ByteBuf, ByteBuf> poolConfig) {
                return hosts -> new SingleHostConnectionProvider<>(hosts.map(hc -> {
                    return new HostConnector<>(hc, PooledConnectionProvider.create(poolConfig,
                        new HostConnector<ByteBuf, ByteBuf>(hc, connectionProvider)));
                }));
            }
        };
        HttpClient   client   = new HttpClient(config, clientProvider, new ObjectMapper(), new RequestParameterSerializers(), Collections.emptySet());
        TestResource resource = client.create(TestResource.class);
        HttpClient.setTimeout(resource, 100, TimeUnit.MILLISECONDS);

        try {
            resource.getHello()
                .delaySubscription(10, TimeUnit.MILLISECONDS)
                .retry(5)
                .toBlocking().last();
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(TimeoutException.class);
        }
        assertThat(connectionsRequested.get()).isEqualTo(6);
    }

    private HttpServer<ByteBuf, ByteBuf> createTestServer(Consumer<String> serverLog) {
        return HttpServer.newServer(0).start((request, response) -> {
            serverLog.accept(request.getDecodedPath());
            if (request.getDecodedPath().equals("/hello/servertest/fast")) {
                response.setStatus(HttpResponseStatus.OK);
                return response.writeString(just("\"fast\""));
            }
            if (request.getDecodedPath().equals("/hello/servertest/slowHeaders")) {
                return Observable.defer(() -> {
                    response.setStatus(HttpResponseStatus.OK);
                    return response.writeString(just("\"slowHeaders\""));
                })
                    .delaySubscription(5000, TimeUnit.MILLISECONDS);
            }
            if (request.getDecodedPath().equals("/hello/servertest/slowBody")) {
                response.setStatus(HttpResponseStatus.OK);
                return response.writeStringAndFlushOnEach(
                    just("\"slowBody: ")
                        .concatWith(just("1", "2", "3").delay(10000, TimeUnit.MILLISECONDS))
                        .concatWith(just("\""))
                );
            }
            return empty();
        });
    }

    @Test
    public void shouldCloseConnectionOnTimeoutDuringContentReceive() throws URISyntaxException {
        Consumer<String>             serverLog = mock(Consumer.class);
        HttpServer<ByteBuf, ByteBuf> server    = createTestServer(serverLog);

        HttpClientConfig config = new HttpClientConfig("localhost:" + server.getServerPort());
        config.setMaxConnections(1);
        HttpClient   client   = new HttpClient(config);
        TestResource resource = client.create(TestResource.class);
        HttpClient.setTimeout(resource, 200, TimeUnit.MILLISECONDS);

        resource.servertest("fast")
            // Delay needed so that repeat will not subscribe immediately, as the pooled connection is released on the event loop thread.
            .delaySubscription(10, TimeUnit.MILLISECONDS)
            .repeat(10)
            .toBlocking()
            .last();

        verify(serverLog, times(10)).accept("/hello/servertest/fast");

        try {
            resource.servertest("slowBody")
                .timeout(50, TimeUnit.MILLISECONDS)
                // Delay needed so that repeat will not subscribe immediately, as the pooled connection is released on the event loop thread.
                .delaySubscription(10, TimeUnit.MILLISECONDS)
                .retry(10)
                .toBlocking()
                .last();
            fail("Expected TimeoutException, but no exception was thrown");
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(TimeoutException.class);
        }

        verify(serverLog, times(11)).accept("/hello/servertest/slowBody");

        server.shutdown();
    }

    @Test
    public void shouldShouldNotGivePoolExhaustedIfServerDoesNotCloseConnection() throws URISyntaxException {

        LogManager.getLogger(HttpClient.class).setLevel(Level.toLevel(Level.OFF_INT));

        HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(0)
            .start((request, response) -> {
                return Observable.defer(() -> {
                    response.setStatus(HttpResponseStatus.OK);
                    return response.writeString(just("\"hello\""));
                })
                    .delaySubscription(50000, TimeUnit.MILLISECONDS)
                    .doOnError(e -> {
                        e.printStackTrace();
                    });
            });

        // this config ensures that the autocleanup will run before the hystrix timeout
        HttpClientConfig config = new HttpClientConfig("localhost:" + server.getServerPort());
        config.setMaxConnections(2);
        HttpClient   client   = new HttpClient(config);
        TestResource resource = client.create(TestResource.class);
        HttpClient.setTimeout(resource, 100, TimeUnit.MILLISECONDS);

        for (int i = 0; i < 5; i++) {
            try {
                resource.getHello().toBlocking().singleOrDefault(null);
                fail("expected exception");
            } catch (Exception e) {
                assertThat(e.getCause()).isInstanceOf(TimeoutException.class);
            }
        }

        server.shutdown();
    }

    @Test
    public void willRequestWithMultipleCookies() {
        Consumer<HttpServerRequest<ByteBuf>> reqLog = mock(Consumer.class);
        HttpServer<ByteBuf, ByteBuf>         server = startServer(OK, "", reqLog::accept);

        String cookie1Value = "stub1";
        String cookie2Value = "stub2";
        String cookieHeader = format("cookie1=%s; cookie2=%s", cookie1Value, cookie2Value);
        getHttpProxy(server.getServerPort()).withMultipleCookies(cookie1Value, cookie2Value).toBlocking().single();

        verify(reqLog).accept(matches(req -> {
            assertThat(req.headerIterator()).isNotEmpty();
            assertThat(req.getHeader("Cookie")).isEqualTo(cookieHeader);
        }));

        server.shutdown();
    }

    private HttpServer<ByteBuf, ByteBuf> startServer(HttpResponseStatus status, String body, Consumer<HttpServerRequest<ByteBuf>> callback) {
        return HttpServer.newServer(0).start((request, response) -> {
            callback.accept(request);
            response.setStatus(status);
            return response.writeString(just(body));
        });
    }

    private HttpServer<ByteBuf, ByteBuf> startServer(HttpResponseStatus status, String body) {
        return startServer(status, body, r -> {
        });
    }

    @Test
    public void shouldSendFormParamsAsBodyWithCorrectContentType() {
        AtomicReference<HttpServerRequest<ByteBuf>> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>                     recordedRequestBody = new AtomicReference<>();

        HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(0).start((request, response) -> {
            recordedRequest.set(request);
            response.setStatus(HttpResponseStatus.CREATED);
            return request.getContent().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Observable.empty();
            });
        });

        TestResource resource = getHttpProxy(server.getServerPort());
        resource.postForm("A", "b!\"#¤%/=&", null, null).toBlocking().singleOrDefault(null);

        assertThat(recordedRequest.get().getHeader("Content-Type")).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        assertThat(recordedRequestBody.get()).isEqualTo("paramA=A&paramB=b%21%22%23%C2%A4%25%2F%3D%26");

        server.shutdown();
    }

    @Test
    public void shouldNotSendHeaderParamsAsPostBody() {
        AtomicReference<HttpServerRequest<ByteBuf>> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>                     recordedRequestBody = new AtomicReference<>();
        HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(0).start((request, response) -> {
            recordedRequest.set(request);
            response.setStatus(HttpResponseStatus.CREATED);
            return request.getContent().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Observable.empty();
            });
        });

        TestResource resource = getHttpProxy(server.getServerPort());
        resource.postForm("A", "b!\"#¤%/=&", "123", "cookie_val").toBlocking().singleOrDefault(null);

        assertThat(recordedRequest.get().getHeader("Content-Type")).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        assertThat(recordedRequest.get().getHeader("myheader")).isEqualTo("123");
        assertThat(recordedRequest.get().getHeader("Cookie")).isEqualTo("cookie_key=cookie_val");
        assertThat(recordedRequestBody.get()).isEqualTo("paramA=A&paramB=b%21%22%23%C2%A4%25%2F%3D%26");

        server.shutdown();
    }

    @Test
    public void shouldUseConsumesAnnotationAsContentTypeHeader() {
        AtomicReference<HttpServerRequest<ByteBuf>> recordedRequest = new AtomicReference<>();
        HttpServer<ByteBuf, ByteBuf>                server          = startServer(OK, "", recordedRequest::set);

        TestResource resource = getHttpProxy(server.getServerPort());
        resource.consumesAnnotation().toBlocking().singleOrDefault(null);

        assertThat(recordedRequest.get().getHeader("Content-Type")).isEqualTo("my-test-value");

        server.shutdown();
    }


    @Test
    public void shouldSetDevParams() throws URISyntaxException {
        AtomicReference<HttpServerRequest<ByteBuf>> recordedRequest = new AtomicReference<>();
        HttpServer<ByteBuf, ByteBuf>                server          = startServer(OK, "", recordedRequest::set);

        HttpClientConfig httpClientConfig = new HttpClientConfig("localhost:12345");
        httpClientConfig.setDevCookie("DevCookie=123");
        ImmutableMap<String, String> devHeader = ImmutableMap.<String, String>builder().put("DevHeader", "213").build();
        httpClientConfig.setDevHeaders(devHeader);
        httpClientConfig.setDevServerInfo(new InetSocketAddress("localhost", server.getServerPort()));

        TestResource resource = getHttpProxy(httpClientConfig);
        resource.withCookie("cookieParam").toBlocking().singleOrDefault(null);

        assertThat(recordedRequest.get().getHeader("Cookie")).isEqualTo("cookie=cookieParam;DevCookie=123");
        assertThat(recordedRequest.get().getHeader("DevHeader")).isEqualTo("213");

        server.shutdown();
    }

    @Test
    public void shouldAllowBodyInPostPutDeletePatchCalls() {
        AtomicReference<HttpServerRequest<ByteBuf>> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>                     recordedRequestBody = new AtomicReference<>();

        HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(0).start((request, response) -> {
            recordedRequest.set(request);
            response.setStatus(HttpResponseStatus.NO_CONTENT);
            return request.getContent().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Observable.empty();
            });
        });

        TestResource resource = getHttpProxy(server.getServerPort());

        resource.patch("test").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("\"test\"");
        assertThat(recordedRequest.get().getHttpMethod()).isEqualTo(HttpMethod.PATCH);

        resource.delete("test").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("\"test\"");
        assertThat(recordedRequest.get().getHttpMethod()).isEqualTo(HttpMethod.DELETE);


        resource.put("test").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("\"test\"");
        assertThat(recordedRequest.get().getHttpMethod()).isEqualTo(HttpMethod.PUT);


        resource.post("test").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("\"test\"");
        assertThat(recordedRequest.get().getHttpMethod()).isEqualTo(HttpMethod.POST);

        server.shutdown();
    }

    @Test
    public void shouldSetTimeoutOnResource() throws URISyntaxException {
        HttpClientConfig httpClientConfig = new HttpClientConfig();
        httpClientConfig.setUrl("http://localhost");

        HttpClient mockClient = Mockito.spy(new HttpClient(httpClientConfig));

        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});
            binder.bind(HttpClient.class).toInstance(mockClient);
        });

        TestResource testResource = injector.getInstance(TestResource.class);
        HttpClient.setTimeout(testResource, 1300, TimeUnit.MILLISECONDS);
        verify(mockClient, times(1)).setTimeout(eq(1300), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldReturnRawResponse() {
        HttpServer<ByteBuf, ByteBuf> server   = startServer(OK, "this is my response");
        TestResource                 resource = getHttpProxy(server.getServerPort());
        HttpClientResponse<ByteBuf>  response = resource.getRawResponse().toBlocking().single();

        assertThat(response.getStatus()).isEqualTo(OK);

        String body = new ByteBufCollector(1024 * 1024).collectString(response.getContent()).toBlocking().single();
        assertThat(body).isEqualTo("this is my response");

        server.shutdown();
    }


    @Test
    public void shouldHandleSimpleGetRequests() {
        HttpServer<ByteBuf, ByteBuf> server   = startServer(OK, "this is my response");

        String url = "http://localhost:" + server.getServerPort();
        String response = HttpClient.get(url).toBlocking().single();

        assertThat(response).isEqualTo("this is my response");

        server.shutdown();
    }

    @Test
    public void shouldErrorHandleSimpleGetRequestsWithBadUrl() {
        HttpServer<ByteBuf, ByteBuf> server   = startServer(OK, "this is my response");

        String url = "htp://localhost:" + server.getServerPort();

        try {
            HttpClient.get(url).toBlocking().single();
        } catch (RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(MalformedURLException.class);
        }

        server.shutdown();
    }

    @Test
    public void shouldExecutePreRequestHooks() throws URISyntaxException {
        HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(0).start((request, response) -> {
            response.setStatus(HttpResponseStatus.OK);
            return response.writeString(just("\"hi\""));
        });

        String           url            = "localhost:" + server.getServerPort();
        HttpClientConfig config         = new HttpClientConfig(url);
        RxClientProvider clientProvider = new RxClientProvider(config, healthRecorder);

        PreRequestHook          preRequestHook  = mock(PreRequestHook.class);
        HashSet<PreRequestHook> preRequestHooks = Sets.newHashSet(preRequestHook);

        HttpClient client = new HttpClient(config, clientProvider, new ObjectMapper(), new RequestParameterSerializers(), preRequestHooks);

        TestResource resource = client.create(TestResource.class);
        resource.getHello().toBlocking().single();

        verify(preRequestHook, times(1)).apply((TestUtil.matches(requestBuilder -> {
            assertThat(requestBuilder.getFullUrl()).isEqualToIgnoringCase(url + "/hello");
        })));

        server.shutdown();
    }

    private String generate10MbString() {
        char[] resp = new char[10 * 1024 * 1024];
        for (int i = 0; i < resp.length; i++) {
            resp[i] = 'a';
        }
        resp[0] = '\"';
        resp[resp.length - 1] = '\"';
        return new String(resp);
    }

    @Path("/hello")
    interface TestResource {
        @GET
        Single<String> getSingle();

        @GET
        Observable<String> getHello();

        @POST
        Observable<String> postHello();

        @Path("{fid}/{key}")
        Observable<String> withPathAndQueryParam(@PathParam("key") String key, @QueryParam("value") String value);

        @Path("multipleQueryParams")
        Observable<String> withMultipleQueryParam(@QueryParam("valueA") String valueA, @QueryParam("valueB") String valueB);

        @Path("{fid}/{key:.*}")
        Observable<String> withRegExpPathAndQueryParam(@PathParam("key") String key, @QueryParam("value") String value);

        @GET
        @Path("/multicookie")
        Observable<byte[]> withMultipleCookies(@CookieParam("cookie1") String param1, @CookieParam("cookie2") String param2);

        @GET
        Observable<String> withCookie(@CookieParam("cookie") String param);

        @GET
        Observable<byte[]> getAsBytes();

        @POST
        Observable<Void> getVoid();

        @POST
        Observable<String> postForm(@FormParam("paramA") String a,
            @FormParam("paramB") String b,
            @HeaderParam("myheader") String header,
            @CookieParam("cookie_key") String cookie
        );

        @POST
        @Consumes("my-test-value")
        Observable<String> consumesAnnotation();

        @GET
        @Path("/servertest/{mode}")
        Observable<String> servertest(@PathParam("mode") String mode);

        @PATCH
        Observable<Void> patch(String value);

        @DELETE
        Observable<Void> delete(String value);

        @PUT
        Observable<Void> put(String value);

        @POST
        Observable<Void> post(String value);

        @GET
        Observable<HttpClientResponse<ByteBuf>> getRawResponse();

        @GET
        Observable<Wrapper> getWrappedPojo();
    }

    class Wrapper {
        private Pojo result;

        public Pojo getResult() {
            return result;
        }

        public void setResult(Pojo result) {
            this.result = result;
        }
    }

    class Pojo {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
