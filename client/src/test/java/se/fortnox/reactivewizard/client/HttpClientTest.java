package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.reactivex.netty.channel.AllocatingTransformer;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.internal.SingleHostConnectionProvider;
import io.reactivex.netty.client.pool.PoolConfig;
import io.reactivex.netty.client.pool.PoolExhaustedException;
import io.reactivex.netty.client.pool.PooledConnectionProvider;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.ws.client.WebSocketRequest;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Single;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.AssertableSubscriber;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.jaxrs.ByteBufCollector;
import se.fortnox.reactivewizard.jaxrs.FieldError;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.PATCH;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import se.fortnox.reactivewizard.server.ServerConfig;
import se.fortnox.reactivewizard.test.LoggingMockUtil;
import se.fortnox.reactivewizard.test.TestUtil;
import se.fortnox.reactivewizard.util.rx.RetryWithDelay;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.BeanParam;
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
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.defer;
import static rx.Observable.empty;
import static rx.Observable.error;
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
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        HttpClient client = new HttpClient(config, new RxClientProvider(config, healthRecorder), mapper, new RequestParameterSerializers(), Collections.emptySet());
        return client.create(TestResource.class);
    }

    private TestResource getHttpProxyWithClientReturningEmpty(AtomicInteger callCounter) {
        HttpClientConfig config = null;
        try {
            config = new HttpClientConfig("localhost:8080");
        } catch (URISyntaxException e) {
            Assert.fail("Could not create httpClientConfig");
        }

        HttpClient client = new HttpClient(config, new RxClientProvider(config, healthRecorder) {
            @Override
            @SuppressWarnings("unchecked")
            public io.reactivex.netty.protocol.http.client.HttpClient<ByteBuf, ByteBuf> clientFor(InetSocketAddress serverInfo) {
                io.reactivex.netty.protocol.http.client.HttpClient mock = mock(io.reactivex.netty.protocol.http.client.HttpClient.class);
                when(mock.createRequest(any(), anyString())).thenAnswer(invocation -> {
                    //callCounter.incrementAndGet();
                    return new EmptyReturningHttpClientRequest(callCounter);
                });
                return mock;
            }

        }, new ObjectMapper(), new RequestParameterSerializers(), Collections.emptySet());
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

                assertThat(e.getCause().getCause().getClass()).isEqualTo(PoolExhaustedException.class);

                assertThat(duration).isGreaterThan(1000);
            }
        });
    }

    //@Test

    /**
     * This test will fail occationally and is therefore commented out.
     * This should be used to try to pin down the bug probably residing in rxnetty
     */
    public void shouldNotSometimesDropItems() {

        withServer(server -> {

            try {
                HttpClientConfig config = new HttpClientConfig("127.0.0.1:" + server.getServerPort());
                config.setRetryCount(1);
                config.setRetryDelayMs(1000);
                config.setMaxConnections(1000);

                TestResource resource = getHttpProxy(config);

                for (int j = 0; j < 300; j++) {
                    List<Observable<String>> results = new ArrayList<>();

                    Thread.sleep(100);
                    int numberOfRequests = 10;
                    for (int i = 0; i < numberOfRequests; i++) {
                        results.add(resource
                            .getHello()
                            .switchIfEmpty(
                                error(new RuntimeException("Did not expect empty"))));
                    }
                    AssertableSubscriber<String> test = Observable.merge(results).test();
                    test.awaitTerminalEvent();
                    test.assertNoErrors();
                }

            } catch (Exception ignore) {
            }

        });
    }

    @Test
    public void shouldRetryIfEmptyReturnedOnGet() {

        AtomicInteger callCount = new AtomicInteger();
        try {
            TestResource resource = getHttpProxyWithClientReturningEmpty(callCount);
            resource.getHello().toBlocking().singleOrDefault(null);
        } catch (Exception expected) {
            assertThat(callCount.get()).isEqualTo(4);
        }
    }

    @Test
    public void shouldNotRetryIfEmptyReturnedOnPost() {

        AtomicInteger callCount = new AtomicInteger();
        try {

            TestResource resource = getHttpProxyWithClientReturningEmpty(callCount);
            resource.postHello().toBlocking().singleOrDefault(null);
        } catch (Exception e) {
            assertThat(callCount.get()).isEqualTo(1);
        }
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

                assertThat(e.getCause().getCause().getClass()).isEqualTo(PoolExhaustedException.class);

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

            assertThat(e.getCause().getCause()).isInstanceOf(ConnectException.class);

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
    public void shouldSupportBeanParam() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        Method     method = TestResource.class.getMethod("withBeanParam", TestResource.Filters.class);
        TestResource.Filters filters = new TestResource.Filters();
        filters.setFilter1("a");
        filters.setFilter2("b");
        String     path   = client.getPath(method, new Object[]{filters}, new JaxRsMeta(method, null));
        assertThat(path).isEqualTo("/hello/beanParam?filter1=a&filter2=b");
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
        final HttpServer<ByteBuf, ByteBuf> server   = startServer(HttpResponseStatus.OK, generateLargeString(10));
        TestResource                       resource = getHttpProxy(server.getServerPort());
        resource.getHello().toBlocking().single();

        server.shutdown();
    }

    @Test
    public void shouldLogErrorOnTooLargeResponse() throws NoSuchFieldException, IllegalAccessException {
        Appender                           mockAppender = LoggingMockUtil.createMockedLogAppender(HttpClient.class);
        final HttpServer<ByteBuf, ByteBuf> server       = startServer(HttpResponseStatus.OK, generateLargeString(11));
        TestResource                       resource     = getHttpProxy(server.getServerPort());
        WebException                       e            = null;
        try {
            resource.getHello().toBlocking().single();
        } catch (WebException we) {
            e = we;
        }

        assertThat(e).isNotNull();
        assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
        assertThat(e.getError()).isEqualTo("too.large.input");

        verify(mockAppender).doAppend(matches(log -> {
            assertThat(log.getMessage()).isEqualTo("Failed request. Url: localhost:" + server.getServerPort() + "/hello, headers: [Host=localhost]");
        }));

        server.shutdown();
    }

    @Test
    public void shouldReturnBadRequestOnTooLargeResponses() throws URISyntaxException {
        final HttpServer<ByteBuf, ByteBuf> server = startServer(HttpResponseStatus.OK, "\"derp\"");
        HttpClientConfig                   config = new HttpClientConfig("127.0.0.1:" + server.getServerPort());
        config.setMaxResponseSize(5);
        TestResource resource = getHttpProxy(config);
        try {
            resource.getHello().toBlocking().single();
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
        }

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
            assertThat(e.getCause().getCause().getCause()).isInstanceOf(JsonMappingException.class);
        }
        assertThat(callCount.get()).isEqualTo(1);

        server.shutdown();
    }

    @Test
    public void shouldParseWebExceptions() {
        AtomicLong callCount = new AtomicLong();
        HttpServer<ByteBuf, ByteBuf> server = startServer(BAD_REQUEST,
            "{\"id\":\"f3872d6a-43b9-41c2-a302-f1fc89621f68\",\"error\":\"validation\",\"fields\":[{\"field\":\"phoneNumber\",\"error\":\"validation.invalid.phone.number\"}]}",
            r -> callCount.incrementAndGet());

        TestResource resource = getHttpProxy(server.getServerPort());
        try {
            resource.getWrappedPojo().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getError()).isEqualTo("validation");
            HttpClient.DetailedError cause = (HttpClient.DetailedError)e.getCause();
            assertThat(cause.getFields()).isNotNull();
            assertThat(cause.getFields()).hasSize(1);
            FieldError error = cause.getFields()[0];
            assertThat(error.getError()).isEqualTo("validation.invalid.phone.number");
            assertThat(error.getField()).isEqualTo("phoneNumber");
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
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
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
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
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
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
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
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
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
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
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
            } catch (WebException e) {
                assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
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
            assertThat(req.headerIterator()).toIterable().isNotEmpty();
            assertThat(req.getHeader("Cookie")).isEqualTo(cookieHeader);
        }));

        server.shutdown();
    }

    private HttpServer<ByteBuf, ByteBuf> startServer(HttpResponseStatus status, String body, Consumer<HttpServerRequest<ByteBuf>> callback) {
        return startServer(status, just(body), callback);
    }

    private HttpServer<ByteBuf, ByteBuf> startServer(HttpResponseStatus status, Observable<String> body, Consumer<HttpServerRequest<ByteBuf>> callback) {
        return HttpServer.newServer(0).start((request, response) -> {
            callback.accept(request);
            response.setStatus(status);
            return response.writeString(body);
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
    public void shouldSendBasicAuthHeaderIfConfigured() throws URISyntaxException {
        AtomicReference<HttpServerRequest<ByteBuf>> recordedRequest = new AtomicReference<>();
        HttpServer<ByteBuf, ByteBuf>                server          = startServer(OK, "", recordedRequest::set);

        HttpClientConfig httpClientConfig = new HttpClientConfig("localhost:" + server.getServerPort());
        httpClientConfig.setBasicAuth("root", "hunter2");

        TestResource resource = getHttpProxy(httpClientConfig);
        resource.getHello().test().awaitTerminalEvent();

        assertThat(recordedRequest.get().containsHeader("Authorization")).isTrue();
        assertThat(recordedRequest.get().getHeader("Authorization")).isEqualTo("Basic cm9vdDpodW50ZXIy");

        server.shutdown();
    }


    @Test
    public void shouldNotSendAuthorizationHeadersUnlessConfigured() throws URISyntaxException {
        AtomicReference<HttpServerRequest<ByteBuf>> recordedRequest = new AtomicReference<>();
        HttpServer<ByteBuf, ByteBuf>                server          = startServer(OK, "", recordedRequest::set);

        HttpClientConfig httpClientConfig = new HttpClientConfig("localhost:" + server.getServerPort());

        TestResource resource = getHttpProxy(httpClientConfig);
        resource.getHello().test().awaitTerminalEvent();

        assertThat(recordedRequest.get().containsHeader("Authorization")).isFalse();

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

            HttpClientProvider httpClientProvider = Mockito.mock(HttpClientProvider.class);
            when(httpClientProvider.createClient(any())).thenReturn(mockClient);

            binder.bind(HttpClientConfig.class).toInstance(httpClientConfig);
            binder.bind(HttpClientProvider.class).toInstance(httpClientProvider);
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
        HttpServer<ByteBuf, ByteBuf> server = startServer(OK, "this is my response");

        String url      = "http://localhost:" + server.getServerPort();
        String response = HttpClient.get(url).toBlocking().single();

        assertThat(response).isEqualTo("this is my response");

        server.shutdown();
    }

    @Test
    public void shouldErrorHandleSimpleGetRequestsWithBadUrl() {
        HttpServer<ByteBuf, ByteBuf> server = startServer(OK, "this is my response");

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

    @Test
    public void shouldHandleHttpsAgainstKnownHost() throws URISyntaxException {
        HttpClientConfig httpClientConfig = new HttpClientConfig("https://sha512.badssl.com/");
        Injector         injector         = injectorWithProgrammaticHttpClientConfig(httpClientConfig);
        RxClientProvider rxClientProvider = injector.getInstance(RxClientProvider.class);

        rxClientProvider
            .clientFor(new InetSocketAddress(httpClientConfig.getHost(), httpClientConfig.getPort()))
            .createGet("/")
            .flatMap(HttpClientResponse::discardContent)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Test
    public void shouldErrorOnUntrustedHost() throws URISyntaxException {
        HttpClientConfig httpClientConfig = new HttpClientConfig("https://untrusted-root.badssl.com");
        Injector         injector         = injectorWithProgrammaticHttpClientConfig(httpClientConfig);
        RxClientProvider rxClientProvider = injector.getInstance(RxClientProvider.class);

        try {
            rxClientProvider
                .clientFor(new InetSocketAddress(httpClientConfig.getHost(), httpClientConfig.getPort()))
                .createGet("/")
                .flatMap(HttpClientResponse::discardContent)
                .toBlocking()
                .singleOrDefault(null);
            fail("Expected SSLHandshakeException");
        } catch (RuntimeException runtimeException) {
            assertThat(runtimeException.getCause()).isInstanceOf(SSLHandshakeException.class);
        }
    }

    @Test
    public void shouldHandleUnsafeSecureOnUntrustedHost() throws URISyntaxException {
        HttpClientConfig httpClientConfig = new HttpClientConfig("https://untrusted-root.badssl.com");
        httpClientConfig.setValidateCertificates(false);
        Injector         injector         = injectorWithProgrammaticHttpClientConfig(httpClientConfig);
        RxClientProvider rxClientProvider = injector.getInstance(RxClientProvider.class);

        rxClientProvider
            .clientFor(new InetSocketAddress(httpClientConfig.getHost(), httpClientConfig.getPort()))
            .createGet("/")
            .flatMap(HttpClientResponse::discardContent)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Test
    public void shouldLogRequestDetailsOnTimeout() {
        HttpServer<ByteBuf, ByteBuf> server = startServer(OK, Observable.never(), r -> {
        });

        try {
            TestResource resource = getHttpProxy(server.getServerPort());
            HttpClient.setTimeout(resource, 10, TimeUnit.MILLISECONDS);
            resource.servertest("mode").toBlocking().single();
            fail("expected timeout");
        } catch (Exception e) {
            OutputStream baos   = new ByteArrayOutputStream();
            PrintStream  stream = new PrintStream(baos, true);
            e.printStackTrace(stream);
            assertThat(baos.toString()).contains("Timeout after 10 ms calling localhost:" + server.getServerPort() + "/hello/servertest/mode");
        } finally {
            server.shutdown();
        }
    }

    private String generateLargeString(int sizeInMB) {
        char[] resp = new char[sizeInMB * 1024 * 1024];
        for (int i = 0; i < resp.length; i++) {
            resp[i] = 'a';
        }
        resp[0] = '\"';
        resp[resp.length - 1] = '\"';
        return new String(resp);
    }

    private Injector injectorWithProgrammaticHttpClientConfig(HttpClientConfig httpClientConfig) {
        return TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});
            binder.bind(HttpClientConfig.class).toInstance(httpClientConfig);
        });
    }

    @Test
    public void assertRequestContainsHost() {
        Consumer<HttpServerRequest<ByteBuf>> reqLog = mock(Consumer.class);
        HttpServer<ByteBuf, ByteBuf>         server = startServer(OK, "", reqLog::accept);

        String host = "localhost";

        getHttpProxy(server.getServerPort()).getHello().toBlocking().singleOrDefault(null);

        verify(reqLog).accept(matches(req -> {
            assertThat(req.headerIterator()).toIterable().isNotEmpty();
            assertThat(req.getHeader("Host")).isEqualTo(host);
        }));

        server.shutdown();
    }

    @Test
    public void assertRequestContainsHostFromHeaderParam() {
        Consumer<HttpServerRequest<ByteBuf>> reqLog = mock(Consumer.class);
        HttpServer<ByteBuf, ByteBuf>         server = startServer(OK, "", reqLog::accept);

        String host = "globalhost";

        getHttpProxy(server.getServerPort()).withHostHeaderParam(host).toBlocking().singleOrDefault(null);

        verify(reqLog).accept(matches(req -> {
            assertThat(req.headerIterator()).toIterable().isNotEmpty();
            assertThat(req.getHeader("Host")).isEqualTo(host);
        }));

        server.shutdown();
    }

    @Test
    public void shouldSupportSendingXml() {
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

        resource.sendXml("<xml></xml>").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("<xml></xml>");
        assertThat(recordedRequest.get().getHttpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(recordedRequest.get().getHeader("Content-Type")).isEqualTo("application/xml");

        server.shutdown();
    }


    @Test
    public void shouldSupportSendingXmlAsBytes() {
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

        resource.sendXml("<xml></xml>".getBytes(Charset.defaultCharset())).toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("<xml></xml>");
        assertThat(recordedRequest.get().getHttpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(recordedRequest.get().getHeader("Content-Type")).isEqualTo("application/xml");

        server.shutdown();
    }

    @Test
    public void shouldFailIfBodyIsNotStringOrBytes() {
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

        try {
            resource.sendXml(new Pojo()).toBlocking().lastOrDefault(null);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("When content type is not application/json the body param must be String or byte[], but was class se.fortnox.reactivewizard.client.HttpClientTest$Pojo");
        }

        server.shutdown();
    }

    @Path("/hello")
    interface TestResource {

        class Filters {
            @QueryParam("filter1")
            private String filter1;

            @QueryParam("filter2")
            private String filter2;

            public String getFilter1() {
                return filter1;
            }

            public void setFilter1(String filter1) {
                this.filter1 = filter1;
            }

            public String getFilter2() {
                return filter2;
            }

            public void setFilter2(String filter2) {
                this.filter2 = filter2;
            }
        }

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

        @Path("beanParam")
        Observable<String> withBeanParam(@BeanParam Filters filters);

        @GET
        @Path("/multicookie")
        Observable<byte[]> withMultipleCookies(@CookieParam("cookie1") String param1, @CookieParam("cookie2") String param2);

        @GET
        @Path("/hostheaderparam")
        Observable<byte[]> withHostHeaderParam(@HeaderParam("Host") String host);

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

        @POST
        @Consumes("application/xml")
        Observable<Void> sendXml(String xml);

        @POST
        @Consumes("application/xml")
        Observable<Void> sendXml(byte[] xml);

        @POST
        @Consumes("application/xml")
        Observable<Void> sendXml(Pojo pojo);
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

    class EmptyReturningHttpClientRequest extends HttpClientRequest<ByteBuf, ByteBuf> {

        public EmptyReturningHttpClientRequest(AtomicInteger callCounter) {
            super(subscriber -> {
                callCounter.incrementAndGet();
                subscriber.onCompleted();
            });
        }

        @Override
        public Observable<HttpClientResponse<ByteBuf>> writeContent(Observable<ByteBuf> contentSource) {
            return empty();
        }

        @Override
        public Observable<HttpClientResponse<ByteBuf>> writeContent(Observable<ByteBuf> contentSource, Func1<ByteBuf, Boolean> flushSelector
        ) {
            return empty();
        }

        @Override
        public <T extends TrailingHeaders> Observable<HttpClientResponse<ByteBuf>> writeContent(Observable<ByteBuf> contentSource,
            Func0<T> trailerFactory,
            Func2<T, ByteBuf, T> trailerMutator
        ) {
            return empty();
        }

        @Override
        public <T extends TrailingHeaders> Observable<HttpClientResponse<ByteBuf>> writeContent(Observable<ByteBuf> contentSource,
            Func0<T> trailerFactory,
            Func2<T, ByteBuf, T> trailerMutator,
            Func1<ByteBuf, Boolean> flushSelector
        ) {
            return empty();
        }

        @Override
        public Observable<HttpClientResponse<ByteBuf>> writeContentAndFlushOnEach(Observable<ByteBuf> contentSource) {
            return empty();
        }

        @Override
        public Observable<HttpClientResponse<ByteBuf>> writeStringContent(Observable<String> contentSource) {
            return empty();
        }

        @Override
        public Observable<HttpClientResponse<ByteBuf>> writeStringContent(Observable<String> contentSource, Func1<String, Boolean> flushSelector
        ) {
            return empty();
        }

        @Override
        public <T extends TrailingHeaders> Observable<HttpClientResponse<ByteBuf>> writeStringContent(Observable<String> contentSource,
            Func0<T> trailerFactory,
            Func2<T, String, T> trailerMutator
        ) {
            return empty();
        }

        @Override
        public <T extends TrailingHeaders> Observable<HttpClientResponse<ByteBuf>> writeStringContent(Observable<String> contentSource,
            Func0<T> trailerFactory,
            Func2<T, String, T> trailerMutator,
            Func1<String, Boolean> flushSelector
        ) {
            return empty();
        }

        @Override
        public Observable<HttpClientResponse<ByteBuf>> writeBytesContent(Observable<byte[]> contentSource) {
            return empty();
        }

        @Override
        public Observable<HttpClientResponse<ByteBuf>> writeBytesContent(Observable<byte[]> contentSource, Func1<byte[], Boolean> flushSelector) {
            return empty();
        }

        @Override
        public <T extends TrailingHeaders> Observable<HttpClientResponse<ByteBuf>> writeBytesContent(Observable<byte[]> contentSource,
            Func0<T> trailerFactory,
            Func2<T, byte[], T> trailerMutator
        ) {
            return empty();
        }

        @Override
        public <T extends TrailingHeaders> Observable<HttpClientResponse<ByteBuf>> writeBytesContent(Observable<byte[]> contentSource,
            Func0<T> trailerFactory,
            Func2<T, byte[], T> trailerMutator,
            Func1<byte[], Boolean> flushSelector
        ) {
            return empty();
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> readTimeOut(int timeOut, TimeUnit timeUnit) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> followRedirects(int maxRedirects) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> followRedirects(boolean follow) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> setMethod(HttpMethod method) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> setUri(String newUri) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> addHeader(CharSequence name, Object value) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> addHeaders(Map<? extends CharSequence, ? extends Iterable<Object>> headers) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> addCookie(Cookie cookie) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> addDateHeader(CharSequence name, Date value) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> addDateHeader(CharSequence name, Iterable<Date> values) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> addHeaderValues(CharSequence name, Iterable<Object> values) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> setDateHeader(CharSequence name, Date value) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> setDateHeader(CharSequence name, Iterable<Date> values) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> setHeader(CharSequence name, Object value) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> setHeaders(Map<? extends CharSequence, ? extends Iterable<Object>> headers) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> setHeaderValues(CharSequence name, Iterable<Object> values) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> removeHeader(CharSequence name) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> setKeepAlive(boolean keepAlive) {
            return this;
        }

        @Override
        public HttpClientRequest<ByteBuf, ByteBuf> setTransferEncodingChunked() {
            return this;
        }

        @Override
        public <II> HttpClientRequest<II, ByteBuf> transformContent(AllocatingTransformer<II, ByteBuf> transformer) {
            return null;
        }

        @Override
        public <OO> HttpClientRequest<ByteBuf, OO> transformResponseContent(Transformer<ByteBuf, OO> transformer) {
            return null;
        }

        @Override
        public WebSocketRequest<ByteBuf> requestWebSocketUpgrade() {
            return null;
        }

        @Override
        public boolean containsHeader(CharSequence name) {
            return false;
        }

        @Override
        public boolean containsHeaderWithValue(CharSequence name, CharSequence value, boolean caseInsensitiveValueMatch) {
            return false;
        }

        @Override
        public String getHeader(CharSequence name) {
            return null;
        }

        @Override
        public List<String> getAllHeaders(CharSequence name) {
            return null;
        }

        @Override
        public Iterator<Map.Entry<CharSequence, CharSequence>> headerIterator() {
            return null;
        }

        @Override
        public Set<String> getHeaderNames() {
            return null;
        }

        @Override
        public HttpVersion getHttpVersion() {
            return null;
        }

        @Override
        public HttpMethod getMethod() {
            return null;
        }

        @Override
        public String getUri() {
            return "";
        }
    }
}
