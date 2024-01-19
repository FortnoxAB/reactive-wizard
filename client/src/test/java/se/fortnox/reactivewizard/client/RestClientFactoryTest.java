package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import rx.Observable;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.binding.scanners.HttpConfigClassScanner;
import se.fortnox.reactivewizard.binding.scanners.JaxRsClassScanner;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.metrics.HealthRecorder;
import se.fortnox.reactivewizard.server.ServerConfig;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RestClientFactoryTest {

    private static final String SRC_TEST_RESOURCES = "src/test/resources/";
    private static final String HTTPCONFIG_URL_YML = SRC_TEST_RESOURCES + "httpconfig_url.yml";

    @Test
    void shouldProxyResourceInterfacesToHttpClient() {

        AtomicReference<HttpClient> httpClientSpy = new AtomicReference<>();

        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});

            HttpClientProvider httpClientProvider = Mockito.mock(HttpClientProvider.class);
            mockResourcesOnClassPath(binder, of(TestResource.class));

            when(httpClientProvider.createClient(any(HttpClientConfig.class))).thenAnswer(invocation -> {
                HttpClientConfig httpClientConfig = invocation.getArgument(0, HttpClientConfig.class);
                httpClientSpy.set(Mockito.spy(new HttpClient(httpClientConfig)));
                return httpClientSpy.get();
            });

            binder.bind(HttpClientProvider.class).toInstance(httpClientProvider);
        }, HTTPCONFIG_URL_YML);

        // Verify that it creates a proxy for the resource
        TestResource testResource = injector.getInstance(TestResource.class);
        verify(httpClientSpy.get(), times(1)).create(any());

        // Verify that calls to the resource pass through the HttpClient
        verify(httpClientSpy.get(), never()).invoke(any(), any(), any());
        testResource.testCall();
        verify(httpClientSpy.get(), times(1)).invoke(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldUseCustomHttpClientConfigWhenRequested() {
        AtomicReference<HttpClient> mockClient = new AtomicReference<>(Mockito.mock(HttpClient.class));
        AtomicReference<HttpClient> mockCustomClient = new AtomicReference<>();

        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});

            HttpClientProvider httpClientProvider = mock(HttpClientProvider.class);

            when(httpClientProvider.createClient(any())).thenAnswer(invocation -> {
                HttpClientConfig httpClientConfig = invocation.getArgument(0, HttpClientConfig.class);
                HttpClient httpClient = spy(new HttpClient(httpClientConfig));
                if (httpClientConfig instanceof CustomHttpClientConfig) {
                    mockCustomClient.set(httpClient);
                    return mockCustomClient.get();
                }
                mockClient.set(httpClient);
                return mockClient.get();
            });

            binder.bind(HttpClientProvider.class).toInstance(httpClientProvider);
        }, HTTPCONFIG_URL_YML);

        // Verify that it creates a proxy for the resource with the customHttpClient
        CustomTestResource customTestResource = injector.getInstance(CustomTestResource.class);
        verify(mockCustomClient.get(), times(1)).create(any());

        // Verify that calls to the resource never pass through the standard HttpClient but the customMade client
        verify(mockCustomClient.get(), never()).invoke(any(), any(), any());
        customTestResource.testCall();
        verify(mockCustomClient.get(), times(1)).invoke(any(), any(), any());

        verifyNoInteractions(mockClient.get());

        //Resetting the custom mock to be able
        reset(mockCustomClient.get());

        TestResource testResource = injector.getInstance(TestResource.class);
        verify(mockClient.get(), times(1)).create(any());

        testResource.testCall();
        verify(mockClient.get(), times(1)).invoke(any(), any(), any());
        verifyNoInteractions(mockCustomClient.get());

        reset(mockClient.get());
    }

    @Test
    void shouldThrowExceptionWhenPointedOutClassIsNotInterface() {
        try {
            HttpConfigClassScanner mock = Mockito.mock(HttpConfigClassScanner.class);
            when(mock.getClasses()).thenReturn(of(TestConfig.class));
            RestClientFactory restClientFactory = new RestClientFactory(
                    Mockito.mock(JaxRsClassScanner.class),
                    mock);

            Module module = new AbstractModule() {
                @Override
                protected void configure() {

                    bind(String[].class).annotatedWith(Names.named("args")).toInstance(new String[]{HTTPCONFIG_URL_YML});
                    bind(ServerConfig.class).toInstance(new ServerConfig() {{
                        setEnabled(false);
                    }});

                    HttpClientProvider httpClientProvider = Mockito.mock(HttpClientProvider.class);
                    when(httpClientProvider.createClient(any())).thenReturn(Mockito.mock(HttpClient.class));
                    bind(HttpClientProvider.class).toInstance(httpClientProvider);
                }
            };
            Guice.createInjector(new AutoBindModules(Modules.override(module).with(restClientFactory)));
            Assertions.fail("Should throw exception when class is not an interface");
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("class se.fortnox.reactivewizard.client.RestClientFactoryTest pointed out in UseInResource annotation must be an interface");
        }
    }

    @Test
    void shouldDeserializeNullToEmpty() throws URISyntaxException {
        DisposableServer server = HttpServer.create().port(0).handle((req, resp) -> resp.sendString(Mono.just("null"))).bindNow();
        try {
            HttpClientConfig config = new HttpClientConfig("http://localhost:"+server.port());
            HttpClientProvider provider = new HttpClientProvider(new ObjectMapper(), new RequestParameterSerializers(), Collections.emptySet(), new HealthRecorder(), new RequestLogger());
            HttpClient client = provider.createClient(config);
            TestResource resource = client.create(TestResource.class);

            assertThat(resource.testCall().isEmpty().toBlocking().single()).isTrue();
        } finally {
            server.disposeNow();
        }
    }


    private void mockResourcesOnClassPath(Binder binder, Set<Class<?>> classes) {
        JaxRsClassScanner mock = Mockito.mock(JaxRsClassScanner.class);
        when(mock.getClasses()).thenReturn(classes);
        binder.bind(JaxRsClassScanner.class).toInstance(mock);
    }

    @Path("")
    public interface TestResource {
        @GET
        Observable<String> testCall();
    }

    @Path("")
    public interface CustomTestResource {
        @GET
        Observable<String> testCall();
    }

    @UseInResource(RestClientFactoryTest.class)
    public static class TestConfig {

    }

}
