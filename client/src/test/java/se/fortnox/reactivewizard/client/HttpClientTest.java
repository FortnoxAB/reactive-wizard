package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import rx.Observable;
import rx.Single;
import rx.observers.AssertableSubscriber;
import se.fortnox.reactivewizard.CollectionOptions;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.jaxrs.FieldError;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.PATCH;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import se.fortnox.reactivewizard.server.ServerConfig;
import se.fortnox.reactivewizard.test.LoggingMockUtil;
import se.fortnox.reactivewizard.test.LoggingVerifier;
import se.fortnox.reactivewizard.test.TestUtil;
import se.fortnox.reactivewizard.test.observable.ObservableAssertions;
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
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static java.lang.String.format;
import static java.util.Collections.EMPTY_SET;
import static java.util.Optional.ofNullable;
import static java.util.stream.IntStream.range;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static org.apache.logging.log4j.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Flux.defer;
import static reactor.core.publisher.Flux.empty;
import static reactor.core.publisher.Flux.just;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

public class HttpClientTest {

    private HealthRecorder healthRecorder = new HealthRecorder();
    private RequestLogger  requestLogger;
    @Rule
    public LoggingVerifier loggingVerifier = new LoggingVerifier(HttpClient.class);

    @Test
    public void shouldNotRetryFailedPostCalls() {
        AtomicLong                   callCount = new AtomicLong();
        DisposableServer server    = startServer(INTERNAL_SERVER_ERROR, "\"NOT OK\"", r -> callCount.incrementAndGet());

        long start = System.currentTimeMillis();
        try {
            TestResource resource = getHttpProxy(server.port());

            resource.postHello().toBlocking().lastOrDefault(null);

            Assert.fail("Expected exception");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;

            assertThat(duration).isLessThan(1000);
            assertThat(callCount.get()).isEqualTo(1);

            assertThat(e.getClass()).isEqualTo(WebException.class);
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldNotRetryFailedPostCallsWithNonWebExceptions() {
        AtomicLong callCount = new AtomicLong();
        DisposableServer server    = startSlowServer(OK, 6, r -> callCount.incrementAndGet());

        try {
            HttpClientConfig config = new HttpClientConfig("localhost:" + server.port());
            config.setReadTimeoutMs(5);
            TestResource resource = getHttpProxy(config);

            resource.postHello().toBlocking().singleOrDefault(null);

            Assert.fail("Expected exception");
        } catch (Exception e) {
            assertThat(callCount.get()).isLessThanOrEqualTo(1);
            assertThat(e.getClass()).isEqualTo(WebException.class);
        } finally {
            server.disposeNow();
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
        requestLogger = new RequestLogger();
        HttpClient client = new HttpClient(config, new ReactorRxClientProvider(config, healthRecorder), mapper, new RequestParameterSerializers(
            Set.of(new SessionParameterSerializer())), Collections.emptySet(), requestLogger);
        return client.create(TestResource.class);
    }

    private TestResource getHttpProxyWithClientReturningEmpty(DisposableServer disposableServer) {

        HttpClientConfig config = null;
        try {
            config = new HttpClientConfig(disposableServer.host() + ":" +disposableServer.port());
        } catch (URISyntaxException e) {
            Assert.fail("Could not create httpClientConfig: "+e);
        }

        HttpClient client = new HttpClient(config, new ReactorRxClientProvider(config, healthRecorder),
            new ObjectMapper(), new RequestParameterSerializers(), Collections.emptySet(), new RequestLogger());
        return client.create(TestResource.class);
    }

    protected void withServer(Consumer<DisposableServer> serverConsumer) {
        final DisposableServer server = startServer(HttpResponseStatus.OK, "\"OK\"");

        LoggingMockUtil.setLevel(RetryWithDelay.class, Level.OFF);

        try {
            serverConsumer.accept(server);
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldRetryOnFullConnectionPool() {
        withServer(server -> {
            long start = System.currentTimeMillis();
            try {
                HttpClientConfig config = new HttpClientConfig("127.0.0.1:" + server.port());
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


            } catch (Exception e) {
                Assert.fail("Expected no exception but got:" + e);
            }
        });
    }


    @Test
    public void shouldNotSometimesDropItems() {

        withServer(server -> {

            try {
                HttpClientConfig config = new HttpClientConfig("127.0.0.1:" + server.port());
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
                                Observable.error(new RuntimeException("Did not expect empty"))));
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
    public void shouldReuseJaxRsMetaObjectsToAvoidReflection() throws URISyntaxException, NoSuchMethodException {

        List<JaxRsMeta> jaxRsMetas = new ArrayList<>();
        HttpClient httpClient = new HttpClient(new HttpClientConfig("localhost")) {
            @Override
            protected JaxRsMeta getJaxRsMeta(Method method) {
                JaxRsMeta jaxRsMeta = super.getJaxRsMeta(method);
                jaxRsMetas.add(jaxRsMeta);
                return jaxRsMeta;
            }
        };

        Method getHello = TestResource.class.getMethod("getHello");
        Method postHello = TestResource.class.getMethod("postHello");

        httpClient.createRequest(getHello,new Object[0]);
        httpClient.createRequest(getHello,new Object[0]);

        httpClient.createRequest(postHello,new Object[0]);
        httpClient.createRequest(postHello,new Object[0]);

        assertThat(jaxRsMetas.get(0)).isSameAs(jaxRsMetas.get(1));
        assertThat(jaxRsMetas.get(2)).isSameAs(jaxRsMetas.get(3));
    }

    @Test
    public void shouldRetryIfEmptyReturnedOnGet() {

        AtomicInteger callCount = new AtomicInteger();

        DisposableServer server = createTestServer(s -> callCount.incrementAndGet());
        try {
            TestResource resource = getHttpProxyWithClientReturningEmpty(server);
            resource.getHello().toBlocking().singleOrDefault(null);
        } catch (Exception expected) {
            assertThat(callCount.get()).isEqualTo(4);
        }
        finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldNotRetryIfEmptyReturnedOnPost() {

        AtomicInteger callCount = new AtomicInteger();
        DisposableServer server = startServer(CREATED, Mono.empty(), s -> callCount.incrementAndGet());
        try {

            TestResource resource = getHttpProxyWithClientReturningEmpty(server);
            resource.postHello().toBlocking().singleOrDefault(null);
        } catch (Exception e) {
            assertThat(callCount.get()).isEqualTo(1);
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldNotReportUnhealthyWhenPoolIsExhaustedRatherSequentiatingTheRequests() throws URISyntaxException {

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
                HttpClientConfig config = new HttpClientConfig("127.0.0.1:" + server.port());
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

            } catch (Exception e) {
                Assert.fail("Expected no exception");
            }
        });
    }

    @Test
    public void shouldReportUnhealthyWhenConnectionCannotBeAcquiredBeforeTimeoutAndAfter10Attempts() throws URISyntaxException {

        HttpClientConfig httpClientConfig = new HttpClientConfig();
        int port = 13337;
        httpClientConfig.setUrl("http://localhost:" + port);
        httpClientConfig.setMaxConnections(1);
        httpClientConfig.setConnectionMaxIdleTimeInMs(10);

        ReactorRxClientProvider reactorRxClientProvider = new ReactorRxClientProvider(httpClientConfig, healthRecorder);
        HttpClient reactorHttpClient = new HttpClient(httpClientConfig, reactorRxClientProvider, new ObjectMapper(),
            new RequestParameterSerializers(), EMPTY_SET, requestLogger);

        TestResource testResource = reactorHttpClient.create(TestResource.class);

        //First ten should not cause the client to be unhealthy
        range(1, 11)
            .forEach(value -> testResource.postHello().test().awaitTerminalEvent());
        assertThat(healthRecorder.isHealthy()).isTrue();

        //but the eleventh should
        testResource.postHello().test().awaitTerminalEvent();
        assertThat(healthRecorder.isHealthy()).isFalse();

        //And a successful connection should reset the healthRecorder to healthy again
        DisposableServer server = HttpServer.create().port(port)
            .handle((request, response) -> defer(() -> {
                response.status(OK);
                return Flux.empty();
            }))
            .bindNow();

        try {
            testResource.postHello().test().awaitTerminalEvent();
            assertThat(healthRecorder.isHealthy()).isTrue();
        } finally {
            server.disposeNow();
        }

        //Sleep over the max idle time
        try {
            Thread.sleep(httpClientConfig.getConnectionMaxIdleTimeInMs() + 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //And should accept another 10 errors
        range(1, 11)
            .forEach(value -> testResource.postHello().test().awaitTerminalEvent());
        assertThat(healthRecorder.isHealthy()).isTrue();
    }

    @Test
    public void shouldReportHealthyWhenPoolIsNotExhausted() {
        withServer(server -> {
            TestResource resource = getHttpProxy(server.port(), 5);

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
        DisposableServer             server            = startServer(HttpResponseStatus.BAD_REQUEST, detailedErrorJson);

        TestResource resource = getHttpProxy(server.port());
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
            server.disposeNow();
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
    public void shouldSupportBeanParamExtendingOtherClasses() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        Method     method = TestResource.class.getMethod("withBeanParam", TestResource.Filters.class);
        TestResource.Filters filters = new TestResource.Filters();
        filters.setFilter1("a");
        filters.setFilter2("b");
        filters.setLimit(10);
        filters.setOffset(15);
        String     path   = client.getPath(method, new Object[]{filters}, new JaxRsMeta(method, null));
        assertThat(path).isEqualTo("/hello/beanParam?limit=10&offset=15&filter2=b&filter1=a");
    }

    @Test
    public void shouldSupportBeanParamRecord() throws Exception {
        HttpClient client = new HttpClient(new HttpClientConfig("localhost"));
        Method     method = TestResource.class.getMethod("withBeanParamRecord", TestResource.SomeRecord.class);
        TestResource.SomeRecord record = new TestResource.SomeRecord("value1", "value2");
        String     path   = client.getPath(method, new Object[]{record}, new JaxRsMeta(method, null));
        assertThat(path).isEqualTo("/hello/beanParamRecord?param2=value2&param1=value1");
    }

    @Test
    public void shouldSupportSingleSource() {
        DisposableServer server = startServer(HttpResponseStatus.OK, "\"OK\"");

        TestResource resource = getHttpProxy(server.port());
        resource.getSingle().toBlocking().value();

        server.disposeNow();
    }

    @Test
    public void shouldReturnFullResponseFromObservable() {
        DisposableServer server = startServer(HttpResponseStatus.OK, Mono.just("\"OK\""), httpServerRequest -> {}, httpServerResponse -> {
            httpServerResponse.addCookie(new DefaultCookie("cookieName", "cookieValue"));
        });

        TestResource resource = getHttpProxy(server.port());

        Response<String> stringResponse = HttpClient.getFullResponse(resource.getHello())
            .toBlocking().singleOrDefault(null);

        assertThat(stringResponse).isNotNull();
        assertThat(stringResponse.getBody()).isEqualTo("OK");
        assertThat(stringResponse.getStatus()).isEqualTo(OK);

        //Case sensitive when getting the entire map structure
        assertThat(stringResponse.getHeaders().get("content-length")).isEqualTo("4");

        //Case insensitive when fetching
        assertThat(stringResponse.getHeader(CONTENT_LENGTH)).isEqualTo("4");

        assertThat(stringResponse.getCookie("cookieName")).hasSize(1);
        assertThat(stringResponse.getCookie("cookieName").get(0)).isEqualTo("cookieValue");
        assertThat(stringResponse.getCookie(null)).hasSize(0);
        assertThat(stringResponse.getCookie("bogus")).hasSize(0);
    }

    @Test
    public void shouldReturnFullResponseFromEmptyObservable() {
        DisposableServer server = startServer(HttpResponseStatus.OK, Mono.just(""), httpServerRequest -> {}, httpServerResponse -> {
            httpServerResponse.addCookie(new DefaultCookie("cookieName", "cookieValue"));
        });

        TestResource resource = getHttpProxy(server.port());

        Response<String> stringResponse = HttpClient.getFullResponse(resource.getHello())
            .toBlocking().singleOrDefault(null);

        assertThat(stringResponse).isNotNull();
        assertThat(stringResponse.getBody()).isNull();
        assertThat(stringResponse.getStatus()).isEqualTo(OK);
    }

    @Test
    public void shouldWrapAndReturnNewFullResponseObservable() {
        DisposableServer server = startServer(HttpResponseStatus.OK, "\"OK\"");

        TestResource resource = getHttpProxy(server.port());

        Observable<String> hello = resource.getHello();

        Response<String> stringResponse = HttpClient.getFullResponse(hello).toBlocking().singleOrDefault(null);
        assertThat(stringResponse).isNotNull();
        assertThat(stringResponse.getBody()).isEqualTo("OK");
        assertThat(stringResponse.getStatus()).isEqualTo(OK);

        //Case sensitive when getting the entire map structure
        assertThat(stringResponse.getHeaders().get("content-length")).isEqualTo("4");

        //Case insensitive when fetching
        assertThat(stringResponse.getHeader(CONTENT_LENGTH)).isEqualTo("4");
    }

    @Test
    public void shouldReturnFullResponseFromSingle() {
        DisposableServer server = startServer(HttpResponseStatus.OK, "\"OK\"");

        TestResource resource = getHttpProxy(server.port());

        Response<String> stringResponse = HttpClient.getFullResponse(resource.getSingle())
            .toBlocking().value();

        assertThat(stringResponse).isNotNull();
        assertThat(stringResponse.getBody()).isEqualTo("OK");
        assertThat(stringResponse.getStatus()).isEqualTo(OK);
        assertThat(stringResponse.getHeaders().get("content-length")).isEqualTo("4");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldWrapAndReturnNewFullResponseSingle() {
        DisposableServer server = startServer(HttpResponseStatus.OK, "\"OK\"");

        TestResource resource = getHttpProxy(server.port());

        Single<String> hello = resource.getSingle();

        Response<String> stringResponse = HttpClient.getFullResponse(hello).toBlocking().value();
        assertThat(stringResponse).isNotNull();
        assertThat(stringResponse.getBody()).isEqualTo("OK");
        assertThat(stringResponse.getStatus()).isEqualTo(OK);

        //Case sensitive when getting the entire map structure
        assertThat(stringResponse.getHeaders().get("content-length")).isEqualTo("4");

        //Case insensitive when fetching
        assertThat(stringResponse.getHeader(CONTENT_LENGTH)).isEqualTo("4");
    }


    @Test
    public void shouldHandleLargeResponses() {
        DisposableServer                   server   = startServer(HttpResponseStatus.OK, generateLargeString(10));
        TestResource                       resource = getHttpProxy(server.port());
        resource.getHello().toBlocking().single();

        server.disposeNow();
    }

    @Test
    public void shouldLogErrorOnTooLargeResponse() {
        DisposableServer server = startServer(HttpResponseStatus.OK, generateLargeString(11));
        try {
            TestResource resource = getHttpProxy(server.port());
            assertThatExceptionOfType(WebException.class)
                .isThrownBy(() -> resource.getHello().toBlocking().single())
                .satisfies(webException -> {
                    assertThat(webException.getStatus())
                        .isEqualTo(BAD_REQUEST);
                    assertThat(webException.getError())
                        .isEqualTo("too.large.input");
                });
            loggingVerifier
                .verify(WARN, "Failed request. Url: localhost:" + server.port() + "/hello, headers: [Host=localhost]");
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldRedactAuthorizationHeaderInLogsAndExceptionMessage() throws Exception {
        DisposableServer server = startServer(BAD_REQUEST, "someError");

        try {
            HttpClientConfig config = new HttpClientConfig("localhost:" + server.port());
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "secretvalue");
            config.setDevHeaders(headers);
            TestResource resource = getHttpProxy(config);

            assertThatExceptionOfType(WebException.class)
                .isThrownBy(() -> resource.getHello()
                    .toBlocking()
                    .single());

            loggingVerifier.verify(WARN, logEvent -> {
                String message = ofNullable(logEvent.getThrown().getMessage())
                    .orElse("");
                assertThat(message)
                    .doesNotContain("secretvalue");
                assertThat(logEvent.getMessage().getFormattedMessage())
                    .isEqualTo("Failed request. Url: localhost:" + server.port() + "/hello, headers: [Authorization=REDACTED, " +
                        "Host=localhost]");
            });
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldApplyHeaderTransformerInLogsAndExceptionMessage() throws Exception {
        DisposableServer server = startServer(BAD_REQUEST, "someError");
        try {
            HttpClientConfig config = new HttpClientConfig("localhost:" + server.port());
            Map<String, String> headers = new HashMap<>();
            headers.put("someHeader", "abcd");
            config.setDevHeaders(headers);
            TestResource resource = getHttpProxy(config);

            requestLogger.addHeaderTransformationClient("someHeader", String::toUpperCase);

            assertThatExceptionOfType(WebException.class)
                .isThrownBy(() -> resource.getHello()
                    .toBlocking()
                    .single());

            loggingVerifier.verify(WARN, "Failed request. Url: localhost:" + server.port() + "/hello, headers: [Host=localhost, someHeader=ABCD]");
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldRedactSensitiveHeaderInLogsAndExceptionMessage() throws Exception {
        DisposableServer server = startServer(BAD_REQUEST, "someError");
        try {
            HttpClientConfig config = new HttpClientConfig("localhost:" + server.port());
            Map<String, String> headers = new HashMap<>();

            headers.put("Cookie", "pepperidge farm");
            config.setDevHeaders(headers);
            TestResource resource = getHttpProxy(config);

            HttpClient.markHeaderAsSensitive(resource, "cookie");

            ObservableAssertions.assertThatExceptionOfType(WebException.class)
                .isEmittedBy(resource.getHello().single());

            loggingVerifier.verify(WARN, logEvent -> {
                String message = ofNullable(logEvent.getThrown().getMessage())
                    .orElse("");
                assertThat(message)
                    .doesNotContain("secretvalue");
                assertThat(logEvent.getMessage().getFormattedMessage())
                    .isEqualTo("Failed request. Url: localhost:" + server.port() + "/hello, headers: [Cookie=REDACTED, Host=localhost]");
            });
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldReturnBadRequestOnTooLargeResponses() throws URISyntaxException {
        DisposableServer                   server = startServer(HttpResponseStatus.OK, "\"derp\"");
        HttpClientConfig                   config = new HttpClientConfig("127.0.0.1:" + server.port());
        config.setMaxResponseSize(5);
        TestResource resource = getHttpProxy(config);
        try {
            resource.getHello().toBlocking().single();
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
        }

        server.disposeNow();
    }

    @Test
    public void shouldSupportByteArrayResponse() {
        DisposableServer                   server   = startServer(HttpResponseStatus.OK, "hej");
        TestResource                       resource = getHttpProxy(server.port());
        byte[]                             result   = resource.getAsBytes().toBlocking().single();
        assertThat(new String(result)).isEqualTo("hej");

        server.disposeNow();
    }

    @Test
    public void shouldRetry5XXResponses() throws URISyntaxException {
        AtomicLong                   callCount = new AtomicLong();
        DisposableServer server    = startServer(INTERNAL_SERVER_ERROR, "", r -> callCount.incrementAndGet());

        HttpClientConfig config = new HttpClientConfig("127.0.0.1:" + server.port());
        config.setRetryDelayMs(10);
        TestResource resource = getHttpProxy(config);
        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
        }
        assertThat(callCount.get()).isEqualTo(4);

        server.disposeNow();
    }

    @Test
    public void shouldNotRetryFor4XXResponses() {
        AtomicLong                   callCount = new AtomicLong();
        DisposableServer             server    = startServer(NOT_FOUND, "", r -> callCount.incrementAndGet());

        TestResource resource = getHttpProxy(server.port());
        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(HttpResponseStatus.NOT_FOUND);
        }
        assertThat(callCount.get()).isEqualTo(1);

        server.disposeNow();
    }

    @Test
    public void shouldNotRetryForJsonParseErrors() {
        AtomicLong                   callCount = new AtomicLong();
        DisposableServer             server    = startServer(OK, "{\"result\":[]}", r -> callCount.incrementAndGet());

        TestResource resource = getHttpProxy(server.port());
        try {
            resource.getWrappedPojo().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause().getCause().getCause()).isInstanceOf(JsonMappingException.class);
        }
        assertThat(callCount.get()).isEqualTo(1);

        server.disposeNow();
    }

    @Test
    public void shouldParseWebExceptions() {
        AtomicLong callCount = new AtomicLong();
        DisposableServer server = startServer(BAD_REQUEST,
            "{\"id\":\"f3872d6a-43b9-41c2-a302-f1fc89621f68\",\"error\":\"validation\",\"fields\":[{\"field\":\"phoneNumber\",\"error\":\"validation.invalid.phone.number\"}]}",
            r -> callCount.incrementAndGet());

        TestResource resource = getHttpProxy(server.port());
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
        server.disposeNow();
    }

    @Test
    public void shouldNotRetryOnTimeout() {
        AtomicLong callCount = new AtomicLong();
        DisposableServer server = startSlowServer(HttpResponseStatus.NOT_FOUND, 1000, r -> callCount.incrementAndGet());

        TestResource resource = getHttpProxy(server.port());
        HttpClient.setTimeout(resource, 500, ChronoUnit.MILLIS);
        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
        }
        assertThat(callCount.get()).isEqualTo(1);

        server.disposeNow();
    }

    @Test
    public void shouldHandleLongerRequestsThan10SecondsWhenRequested() {
        DisposableServer server = startSlowServer(HttpResponseStatus.NOT_FOUND, 20000);

        TestResource resource = getHttpProxy(server.port(), 1, 30000);
        HttpClient.setTimeout(resource, 15000, ChronoUnit.MILLIS);
        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
        }

        server.disposeNow();
    }

    @Test
    public void shouldThrowNettyReadTimeoutIfRequestTakesLongerThanClientIsConfigured() {
        // Slow server
        DisposableServer server = HttpServer.create().port(0)
            .handle((request, response) -> Flux.defer(() -> {
                response.status(HttpResponseStatus.NOT_FOUND);
                return Flux.<Void>empty();
            }).delaySubscription(Duration.ofMillis(1000)))
            .bindNow();

        //Create a resource with 500ms limit
        TestResource resource = getHttpProxy(server.port(), 1, 500);

        //Lets set the observable timeout higher than the httpproxy readTimeout
        HttpClient.setTimeout(resource, 1000, ChronoUnit.MILLIS);

        try {
            resource.getHello().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (WebException e) {
            assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
        }

        server.disposeNow();
    }

    @Test
    public void shouldHandleMultipleChunks() {
        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            response.status(HttpResponseStatus.OK);
            return response.sendString(just("\"he")
                .concatWith(defer(() -> just("llo\"")))
                .concatWith(defer(Flux::empty)));
        }).bindNow();

        TestResource resource = getHttpProxy(server.port());
        String       result   = resource.getHello().toBlocking().single();
        assertThat(result).isEqualTo("hello");

        server.disposeNow();
    }

    @Test
    public void shouldDeserializeVoidResult() {
        DisposableServer server = startServer(HttpResponseStatus.CREATED, "");

        TestResource resource = getHttpProxy(server.port());
        resource.getVoid().toBlocking().singleOrDefault(null);

        server.disposeNow();
    }

    @Test
    public void shouldDeserializeSimpleStringWithoutJsonQuotes() {
        final String     body   = UUID.randomUUID().toString();
        DisposableServer server = startServer(HttpResponseStatus.CREATED, body);

        TestResource resource = getHttpProxy(server.port());
        final String s        = resource.getString().toBlocking().singleOrDefault(null);

        assertThat(s).isEqualTo(body);
        server.disposeNow();
    }

    @Test
    public void shouldShutDownConnectionOnTimeoutBeforeHeaders() throws URISyntaxException {
        Consumer<String>             serverLog = mock(Consumer.class);
        DisposableServer server    = createTestServer(serverLog);

        HttpClientConfig config = new HttpClientConfig("localhost:" + server.port());
        config.setMaxConnections(1);
        config.setRetryCount(0);
        HttpClient   client   = new HttpClient(config);
        TestResource resource = client.create(TestResource.class);
        HttpClient.setTimeout(resource, 200, ChronoUnit.MILLIS);

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

        server.disposeNow();
    }

    private DisposableServer createTestServer(Consumer<String> serverLog) {
        return HttpServer.create().host("localhost").port(0).handle((request, response) -> {
            serverLog.accept(request.fullPath());
            if (request.fullPath().equals("/hello/servertest/fast")) {
                response.status(HttpResponseStatus.OK);
                return response.sendString(just("\"fast\""));
            }
            if (request.fullPath().equals("/hello/servertest/slowHeaders")) {
                return Flux.defer(() -> {
                    response.status(HttpResponseStatus.OK);
                    return response.sendString(just("\"slowHeaders\""));
                })
                    .delaySubscription(Duration.ofMillis(5000));
            }
            if (request.fullPath().equals("/hello/servertest/slowBody")) {
                response.status(HttpResponseStatus.OK);
                return response.sendString(
                    just("\"slowBody: ")
                        .concatWith(just("1", "2", "3").delaySequence(Duration.ofMillis(10000)))
                        .concatWith(just("\""))
                );
            }
            return empty();
        }).bindNow();
    }

    @Test
    public void shouldCloseConnectionOnTimeoutDuringContentReceive() throws URISyntaxException {
        Consumer<String>             serverLog = mock(Consumer.class);
        DisposableServer server    = createTestServer(serverLog);

        HttpClientConfig config = new HttpClientConfig("localhost:" + server.port());
        config.setMaxConnections(1);
        HttpClient   client   = new HttpClient(config);
        TestResource resource = client.create(TestResource.class);
        HttpClient.setTimeout(resource, 200, ChronoUnit.MILLIS);

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

        server.disposeNow();
    }

    @Test
    public void shouldShouldNotGivePoolExhaustedIfServerDoesNotCloseConnection() throws URISyntaxException {

        Level originalLogLevel = LoggingMockUtil.setLevel(HttpClient.class, Level.OFF);

        try {
            DisposableServer server = HttpServer.create().port(0)
                .handle((request, response) -> {
                    return Flux.defer(() -> {
                        response.status(HttpResponseStatus.OK);
                        return response.sendString(Mono.just("\"hello\""));
                    })
                        .delaySubscription(Duration.ofMillis(50000))
                        .doOnError(e -> {
                            e.printStackTrace();
                        });
                }).bindNow();

            // this config ensures that the autocleanup will run before the hystrix timeout
            HttpClientConfig config = new HttpClientConfig("localhost:" + server.port());
            config.setMaxConnections(2);
            HttpClient   client   = new HttpClient(config);
            TestResource resource = client.create(TestResource.class);
            HttpClient.setTimeout(resource, 100, ChronoUnit.MILLIS);

            for (int i = 0; i < 5; i++) {
                try {
                    resource.getHello().toBlocking().singleOrDefault(null);
                    fail("expected exception");
                } catch (WebException e) {
                    assertThat(e.getStatus()).isEqualTo(GATEWAY_TIMEOUT);
                }
            }

            server.disposeNow();
        } finally {
            LoggingMockUtil.setLevel(HttpClient.class, originalLogLevel);
        }
    }

    @Test
    public void willRequestWithMultipleCookies() {
        Consumer<HttpServerRequest> reqLog = mock(Consumer.class);
        DisposableServer         server = startServer(OK, "", reqLog::accept);

        String cookie1Value = "stub1";
        String cookie2Value = "stub2";
        String cookieHeader = format("cookie1=%s; cookie2=%s", cookie1Value, cookie2Value);
        getHttpProxy(server.port()).withMultipleCookies(cookie1Value, cookie2Value).toBlocking().single();

        verify(reqLog).accept(matches(req -> {
            assertThat(req.requestHeaders()).isNotEmpty();
            assertThat(req.requestHeaders().get("Cookie")).isEqualTo(cookieHeader);
        }));

        server.disposeNow();
    }

    private DisposableServer startServer(HttpResponseStatus status, String body, Consumer<HttpServerRequest> callback) {
        return startServer(status, Mono.just(body), callback, httpServerResponse -> {});
    }

    private DisposableServer startServer(HttpResponseStatus status, Publisher<String> body, Consumer<HttpServerResponse> responseCallback) {
        return startServer(status, body, request -> {}, responseCallback);
    }

    private DisposableServer startServer(HttpResponseStatus status, Publisher<String> body, Consumer<HttpServerRequest> callback, Consumer<HttpServerResponse> responseCallback) {
        return HttpServer.create().host("localhost").port(0).handle((request, response) -> {
            callback.accept(request);
            response.status(status);
            responseCallback.accept(response);
            return response.sendString(body);
        }).bindNow();
    }

    private DisposableServer startServer(HttpResponseStatus status, String body) {
        return startServer(status, body, r -> {
        });
    }

    private DisposableServer startSlowServer(HttpResponseStatus status, Integer delayInMs) {
        return startSlowServer(status, delayInMs, r -> {});
    }

    private DisposableServer startSlowServer(HttpResponseStatus status, Integer delayInMs, Consumer<HttpServerRequest> callback) {
        return HttpServer.create().port(0)
            .handle((request, response) -> {
                callback.accept(request);
                return Flux.defer(() -> {
                    response.status(status);
                    return Flux.<Void>empty();
                }).delaySubscription(Duration.ofMillis(delayInMs));
            }).bindNow();
    }

    @Test
    public void shouldSendFormParamsAsBodyWithCorrectContentType() {
        AtomicReference<HttpServerRequest> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>                     recordedRequestBody = new AtomicReference<>();

        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            recordedRequest.set(request);
            response.status(HttpResponseStatus.CREATED);
            return request.receive().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Flux.empty();
            });
        }).bindNow();

        TestResource resource = getHttpProxy(server.port());
        resource.postForm("A", "b!\"#¤%/=&", null, null).toBlocking().singleOrDefault(null);

        assertThat(recordedRequest.get().requestHeaders().get("Content-Type")).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        assertThat(recordedRequestBody.get()).isEqualTo("paramA=A&paramB=b%21%22%23%C2%A4%25%2F%3D%26");

        server.disposeNow();
    }

    @Test
    public void shouldNotSendHeaderParamsAsPostBody() {
        AtomicReference<HttpServerRequest> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>                     recordedRequestBody = new AtomicReference<>();
        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            recordedRequest.set(request);
            response.status(HttpResponseStatus.CREATED);
            return request.receive().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Flux.empty();
            });
        }).bindNow();

        TestResource resource = getHttpProxy(server.port());
        resource.postForm("A", "b!\"#¤%/=&", "123", "cookie_val").toBlocking().singleOrDefault(null);

        assertThat(recordedRequest.get().requestHeaders().get("Content-Type")).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        assertThat(recordedRequest.get().requestHeaders().get("myheader")).isEqualTo("123");
        assertThat(recordedRequest.get().requestHeaders().get("Cookie")).isEqualTo("cookie_key=cookie_val");
        assertThat(recordedRequestBody.get()).isEqualTo("paramA=A&paramB=b%21%22%23%C2%A4%25%2F%3D%26");

        server.disposeNow();
    }

    @Test
    public void shouldNotWriteParametersWithRequestParameterSerializersToRequestBody() {
        AtomicReference<HttpServerRequest> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>                     recordedRequestBody = new AtomicReference<>();
        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            recordedRequest.set(request);
            response.status(HttpResponseStatus.CREATED);
            return request.receive().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Flux.empty();
            });
        }).bindNow();

        TestResource resource = getHttpProxy(server.port());

        final SessionImpl session = new SessionImpl();
        resource.postWithRequestAuthenticatorParam(session).toBlocking().singleOrDefault(null);

        assertThat(recordedRequestBody.get()).isNull();
        assertThat(recordedRequest.get().requestHeaders().get("SESSION")).isEqualTo(session.getSessionId().toString());

        server.disposeNow();
    }

    @Test
    public void shouldUseConsumesAnnotationAsContentTypeHeader() {
        AtomicReference<HttpServerRequest> recordedRequest = new AtomicReference<>();
        DisposableServer                   server          = startServer(OK, "", recordedRequest::set);

        TestResource resource = getHttpProxy(server.port());
        resource.consumesAnnotation().toBlocking().singleOrDefault(null);

        assertThat(recordedRequest.get().requestHeaders().get("Content-Type")).isEqualTo("my-test-value");

        server.disposeNow();
    }

    @Test
    public void shouldSendBasicAuthHeaderIfConfigured() throws URISyntaxException {
        AtomicReference<HttpServerRequest> recordedRequest = new AtomicReference<>();
        DisposableServer                   server          = startServer(OK, "", recordedRequest::set);

        HttpClientConfig httpClientConfig = new HttpClientConfig("localhost:" + server.port());
        httpClientConfig.setBasicAuth("root", "hunter2");

        TestResource resource = getHttpProxy(httpClientConfig);
        resource.getHello().test().awaitTerminalEvent();

        assertThat(recordedRequest.get().requestHeaders().contains("Authorization")).isTrue();
        assertThat(recordedRequest.get().requestHeaders().get("Authorization")).isEqualTo("Basic cm9vdDpodW50ZXIy");

        server.disposeNow();
    }


    @Test
    public void shouldNotSendAuthorizationHeadersUnlessConfigured() throws URISyntaxException {
        AtomicReference<HttpServerRequest> recordedRequest = new AtomicReference<>();
        DisposableServer                   server          = startServer(OK, "", recordedRequest::set);

        HttpClientConfig httpClientConfig = new HttpClientConfig("localhost:" + server.port());

        TestResource resource = getHttpProxy(httpClientConfig);
        resource.getHello().test().awaitTerminalEvent();

        assertThat(recordedRequest.get().requestHeaders().contains("Authorization")).isFalse();

        server.disposeNow();
    }

    @Test
    public void shouldSetDevParams() throws URISyntaxException {
        AtomicReference<HttpServerRequest> recordedRequest = new AtomicReference<>();
        DisposableServer                   server          = startServer(OK, "", recordedRequest::set);

        HttpClientConfig httpClientConfig = new HttpClientConfig("localhost:12345");
        httpClientConfig.setDevCookie("DevCookie=123");
        ImmutableMap<String, String> devHeader = ImmutableMap.<String, String>builder().put("DevHeader", "213").build();
        httpClientConfig.setDevHeaders(devHeader);
        httpClientConfig.setDevServerInfo(new InetSocketAddress("localhost", server.port()));

        TestResource resource = getHttpProxy(httpClientConfig);
        resource.withCookie("cookieParam").toBlocking().singleOrDefault(null);

        assertThat(recordedRequest.get().requestHeaders().get("Cookie")).isEqualTo("cookie=cookieParam;DevCookie=123");
        assertThat(recordedRequest.get().requestHeaders().get("DevHeader")).isEqualTo("213");

        server.disposeNow();
    }

    @Test
    public void shouldAllowBodyInPostPutDeletePatchCalls() {
        AtomicReference<HttpServerRequest> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>                     recordedRequestBody = new AtomicReference<>();

        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            recordedRequest.set(request);
            response.status(HttpResponseStatus.NO_CONTENT);
            return request.receive().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Flux.empty();
            });
        }).bindNow();

        TestResource resource = getHttpProxy(server.port());

        resource.patch("test").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("\"test\"");
        assertThat(recordedRequest.get().method()).isEqualTo(HttpMethod.PATCH);

        resource.delete("test").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("\"test\"");
        assertThat(recordedRequest.get().method()).isEqualTo(HttpMethod.DELETE);

        resource.put("test").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("\"test\"");
        assertThat(recordedRequest.get().method()).isEqualTo(HttpMethod.PUT);

        resource.post("test").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("\"test\"");
        assertThat(recordedRequest.get().method()).isEqualTo(HttpMethod.POST);

        server.disposeNow();
    }

    @Test
    public void shouldBeAbleToSendAndReceiveRecordBody() {
        DisposableServer server = HttpServer.create().port(0).handle((request, response) ->
            request.receive().aggregate().flatMap(buf ->
                response.status(HttpResponseStatus.OK).send(Mono.just(buf)).then()
        )).bindNow();

        var resource = getHttpProxy(server.port());

        var requestRecord = new TestResource.SomeRecord("value1", "value2");

        var responseRecord = resource.postRecord(requestRecord).toBlocking().singleOrDefault(null);

        assertThat(responseRecord).isNotNull();
        assertThat(responseRecord).isEqualTo(requestRecord);

        server.disposeNow();
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
        HttpClient.setTimeout(testResource, 1300, ChronoUnit.MILLIS);
        verify(mockClient, times(1)).setTimeout(1300, ChronoUnit.MILLIS);
    }

    @Test
    public void shouldExecutePreRequestHooks() throws URISyntaxException {
        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            response.status(HttpResponseStatus.OK);
            return response.sendString(Mono.just("\"hi\""));
        }).bindNow();

        String           url            = "localhost:" + server.port();
        HttpClientConfig config         = new HttpClientConfig(url);
        ReactorRxClientProvider clientProvider = new ReactorRxClientProvider(config, healthRecorder);

        PreRequestHook          preRequestHook  = mock(PreRequestHook.class);
        HashSet<PreRequestHook> preRequestHooks = Sets.newHashSet(preRequestHook);

        HttpClient client = new HttpClient(config, clientProvider, new ObjectMapper(), new RequestParameterSerializers(), preRequestHooks, new RequestLogger());

        TestResource resource = client.create(TestResource.class);
        resource.getHello().toBlocking().single();

        verify(preRequestHook, times(1)).apply((TestUtil.matches(requestBuilder -> {
            assertThat(requestBuilder.getFullUrl()).isEqualToIgnoringCase(url + "/hello");
        })));

        server.disposeNow();
    }

    @Test
    public void shouldNotRetryFailedPostCallsWithStrangeExceptions() {
        AtomicLong callCount = new AtomicLong();
        DisposableServer server    = startServer(INTERNAL_SERVER_ERROR, "\"NOT OK\"", r -> callCount.incrementAndGet());
        try {
            HttpClientConfig config = new HttpClientConfig("localhost:" + server.port());
            config.setReadTimeoutMs(500);
            TestResource resource = getHttpProxy(config);
            resource.postHello().toBlocking().singleOrDefault(null);
            Assert.fail("Expected exception");
        } catch (Exception e) {
            assertThat(callCount.get()).isEqualTo(1);
            assertThat(e.getClass()).isEqualTo(WebException.class);
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldHandleHttpsAgainstKnownHost() throws URISyntaxException {
        HttpClientConfig httpClientConfig = new HttpClientConfig("https://www.google.com");
        Injector         injector         = injectorWithProgrammaticHttpClientConfig(httpClientConfig);
        ReactorRxClientProvider rxClientProvider = injector.getInstance(ReactorRxClientProvider.class);

        rxClientProvider
            .clientFor(new InetSocketAddress(httpClientConfig.getHost(), httpClientConfig.getPort()))
            .baseUrl(httpClientConfig.getUrl())
            .get().uri("/")
            .responseContent()
            .flatMap(data -> Flux.empty())
            .count()
            .block();
    }

    @Test
    public void shouldErrorOnUntrustedHost() throws URISyntaxException, CertificateException {
        SelfSignedCertificate cert          = new SelfSignedCertificate();
        SslContextBuilder     serverOptions = SslContextBuilder.forServer(cert.certificate(), cert.privateKey());
        DisposableServer server =
            reactor.netty.http.server.HttpServer.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(serverOptions))
                .handle((req, res) -> res.sendString(Mono.just("Hello")))
                .bindNow();

        HttpClientConfig httpClientConfig = new HttpClientConfig("https://localhost:" + server.port());

        Injector         injector         = injectorWithProgrammaticHttpClientConfig(httpClientConfig);
        ReactorRxClientProvider rxClientProvider = injector.getInstance(ReactorRxClientProvider.class);

        try {
            rxClientProvider
                .clientFor(new InetSocketAddress("localhost", server.port()))
                .get()
                .uri("/")
                .responseContent()
                .aggregate()
                .asString()
                .block();
            fail("expected error");
        } catch (RuntimeException runtimeException) {
            assertThat(runtimeException.getCause()).isInstanceOf(SSLHandshakeException.class);
        }

        server.disposeNow();
    }

    @Test
    public void shouldHandleUnsafeSecureOnUntrustedHost() throws URISyntaxException, CertificateException {

        SelfSignedCertificate cert          =new SelfSignedCertificate();
        SslContextBuilder serverOptions = SslContextBuilder.forServer(cert.certificate(), cert.privateKey());
        DisposableServer server =
            reactor.netty.http.server.HttpServer.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(serverOptions))
                .handle((req, res) -> res.sendString(Mono.just("Hello")))
                .bindNow();

        HttpClientConfig httpClientConfig = new HttpClientConfig("https://localhost:" + server.port());
        httpClientConfig.setValidateCertificates(false);
        Injector         injector         = injectorWithProgrammaticHttpClientConfig(httpClientConfig);
        ReactorRxClientProvider rxClientProvider = injector.getInstance(ReactorRxClientProvider.class);

        try {
            String response = rxClientProvider
                .clientFor(new InetSocketAddress("localhost", server.port()))
                .get()
                .uri("/")
                .responseContent()
                .aggregate()
                .asString()
                .block();
            assertThat(response).isEqualTo("Hello");
        } catch (RuntimeException runtimeException) {
            Assert.fail("Should not give an error");
        }

        server.disposeNow();
    }

    @Test
    public void shouldLogRequestDetailsOnTimeout() {
        DisposableServer server = startServer(OK, Flux.never(), r -> {
        });

        try {
            TestResource resource = getHttpProxy(server.port());
            HttpClient.setTimeout(resource, 10, ChronoUnit.MILLIS);
            resource.servertest("mode").toBlocking().single();
            fail("expected timeout");
        } catch (Exception e) {
            OutputStream baos   = new ByteArrayOutputStream();
            PrintStream  stream = new PrintStream(baos, true);
            e.printStackTrace(stream);
            assertThat(baos.toString()).contains("Timeout after 10 ms calling localhost:" + server.port() + "/hello/servertest/mode");
        } finally {
            server.disposeNow();
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
        Consumer<HttpServerRequest> reqLog = mock(Consumer.class);
        DisposableServer            server = startServer(OK, "", reqLog::accept);

        String host = "localhost";

        getHttpProxy(server.port()).getHello().toBlocking().singleOrDefault(null);

        verify(reqLog).accept(matches(req -> {
            assertThat(req.requestHeaders()).isNotEmpty();
            assertThat(req.requestHeaders().get("Host")).isEqualTo(host);
        }));

        server.disposeNow();
    }

    @Test
    public void assertRequestContainsHostFromHeaderParam() {
        Consumer<HttpServerRequest> reqLog = mock(Consumer.class);
        DisposableServer            server = startServer(OK, "", reqLog::accept);

        String host = "globalhost";

        getHttpProxy(server.port()).withHostHeaderParam(host).toBlocking().singleOrDefault(null);

        verify(reqLog).accept(matches(req -> {
            assertThat(req.requestHeaders()).isNotEmpty();
            assertThat(req.requestHeaders().get("Host")).isEqualTo(host);
        }));

        server.disposeNow();
    }

    @Test
    public void shouldSupportSendingXml() {
        AtomicReference<HttpServerRequest> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>                     recordedRequestBody = new AtomicReference<>();

        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            recordedRequest.set(request);
            response.status(HttpResponseStatus.NO_CONTENT);
            return request.receive().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Flux.empty();
            });
        }).bindNow();

        TestResource resource = getHttpProxy(server.port());

        resource.sendXml("<xml></xml>").toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("<xml></xml>");
        assertThat(recordedRequest.get().method()).isEqualTo(HttpMethod.POST);
        assertThat(recordedRequest.get().requestHeaders().get("Content-Type")).isEqualTo("application/xml");

        server.disposeNow();
    }


    @Test
    public void shouldSupportSendingXmlAsBytes() {
        AtomicReference<HttpServerRequest> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>            recordedRequestBody = new AtomicReference<>();

        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            recordedRequest.set(request);
            response.status(HttpResponseStatus.NO_CONTENT);
            return request.receive().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Flux.empty();
            });
        }).bindNow();

        TestResource resource = getHttpProxy(server.port());

        resource.sendXml("<xml></xml>".getBytes(Charset.defaultCharset())).toBlocking().lastOrDefault(null);
        assertThat(recordedRequestBody.get()).isEqualTo("<xml></xml>");
        assertThat(recordedRequest.get().method()).isEqualTo(HttpMethod.POST);
        assertThat(recordedRequest.get().requestHeaders().get("Content-Type")).isEqualTo("application/xml");

        server.disposeNow();
    }

    @Test
    public void shouldFailIfBodyIsNotStringOrBytes() {
        AtomicReference<HttpServerRequest> recordedRequest     = new AtomicReference<>();
        AtomicReference<String>                     recordedRequestBody = new AtomicReference<>();

        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            recordedRequest.set(request);
            response.status(HttpResponseStatus.NO_CONTENT);
            return request.receive().flatMap(buf -> {
                recordedRequestBody.set(buf.toString(Charset.defaultCharset()));
                return Flux.empty();
            });
        }).bindNow();

        TestResource resource = getHttpProxy(server.port());

        try {
            resource.sendXml(new Pojo()).toBlocking().lastOrDefault(null);
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("When content type is not application/json the body param must be String or byte[], but was class se.fortnox.reactivewizard.client.HttpClientTest$Pojo");
        }

        server.disposeNow();
    }

    @Path("/hello")
    public interface TestResource {

        class Filters extends CollectionOptions {
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

        record SomeRecord(
            @QueryParam("param1") String param1,
            @QueryParam("param2") String param2
        ) {
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

        @Path("beanParamRecord")
        Observable<String> withBeanParamRecord(@BeanParam SomeRecord record);

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

        @GET
        Observable<String> getString();

        @POST
        Observable<String> postForm(@FormParam("paramA") String a,
            @FormParam("paramB") String b,
            @HeaderParam("myheader") String header,
            @CookieParam("cookie_key") String cookie
        );

        @POST
        Observable<String> postWithRequestAuthenticatorParam(SessionInterface session);

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
        Observable<Wrapper> getWrappedPojo();

        @POST
        @Path("postRecord")
        Observable<SomeRecord> postRecord(SomeRecord record);

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

    static class Wrapper {
        private Pojo result;

        public Pojo getResult() {
            return result;
        }

        public void setResult(Pojo result) {
            this.result = result;
        }
    }

    static class Pojo {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private interface SessionInterface {
        UUID getSessionId();
    }

    private static class SessionImpl implements SessionInterface {

        private UUID sessionId = UUID.randomUUID();

        @Override
        public UUID getSessionId() {
            return this.sessionId;
        }
    }

    private static class SessionParameterSerializer implements RequestParameterSerializer<SessionInterface> {

        @Override
        public void addParameter(SessionInterface param, RequestBuilder request) {
            request.addHeader("SESSION", param.getSessionId().toString());
        }
    }
}
