package se.fortnox.reactivewizard.reactorclient;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.binding.scanners.HttpConfigClassScanner;
import se.fortnox.reactivewizard.binding.scanners.JaxRsClassScanner;
import se.fortnox.reactivewizard.client.HttpClientConfig;
import se.fortnox.reactivewizard.client.UseInResource;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.server.ServerConfig;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.collect.ImmutableSet.of;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RestClientFactoryTest {

    private static final String SRC_TEST_RESOURCES = "src/test/resources/";
    private static final String HTTPCONFIG_URL_YML = SRC_TEST_RESOURCES + "httpconfig_url.yml";

    @Test
    public void shouldProxyResourceInterfacesToReactorHttpClient() {

        AtomicReference<ReactorHttpClient> httpClientSpy = new AtomicReference<>();

        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});

            ReactorHttpClientProvider httpClientProvider = Mockito.mock(ReactorHttpClientProvider.class);
            mockResourcesOnClassPath(binder, of(TestResource.class));

            when(httpClientProvider.createClient(any(HttpClientConfig.class))).thenAnswer(invocation -> {
                HttpClientConfig httpClientConfig = invocation.getArgumentAt(0, HttpClientConfig.class);
                httpClientSpy.set(Mockito.spy(new ReactorHttpClient(httpClientConfig)));
                return httpClientSpy.get();
            });

            binder.bind(ReactorHttpClientProvider.class).toInstance(httpClientProvider);
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
    public void shouldUseCustomHttpClientConfigWhenRequested() {
        AtomicReference<ReactorHttpClient> mockClient = new AtomicReference<>(Mockito.mock(ReactorHttpClient.class));
        AtomicReference<ReactorHttpClient> mockCustomClient = new AtomicReference<>();

        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});

            ReactorHttpClientProvider httpClientProvider = mock(ReactorHttpClientProvider.class);

            when(httpClientProvider.createClient(any())).thenAnswer(invocation -> {
                HttpClientConfig httpClientConfig = invocation.getArgumentAt(0, HttpClientConfig.class);
                ReactorHttpClient httpClient = spy(new ReactorHttpClient(httpClientConfig));
                if (httpClientConfig instanceof CustomHttpClientConfig) {
                    mockCustomClient.set(httpClient);
                    return mockCustomClient.get();
                }
                mockClient.set(httpClient);
                return mockClient.get();
            });

            binder.bind(ReactorHttpClientProvider.class).toInstance(httpClientProvider);
        }, HTTPCONFIG_URL_YML);

        // Verify that it creates a proxy for the resource with the customHttpClient
        CustomTestResource customTestResource = injector.getInstance(CustomTestResource.class);
        verify(mockCustomClient.get(), times(1)).create(any());

        // Verify that calls to the resource never pass through the standard HttpClient but the customMade client
        verify(mockCustomClient.get(), never()).invoke(any(), any(), any());
        customTestResource.testCall();
        verify(mockCustomClient.get(), times(1)).invoke(any(), any(), any());

        verifyZeroInteractions(mockClient.get());

        //Resetting the custom mock to be able
        reset(mockCustomClient.get());

        TestResource testResource = injector.getInstance(TestResource.class);
        verify(mockClient.get(), times(1)).create(any());

        testResource.testCall();
        verify(mockClient.get(), times(1)).invoke(any(), any(), any());
        verifyZeroInteractions(mockCustomClient.get());

        reset(mockClient.get());
    }

    @Test
    public void shouldThrowExceptionWhenPointedOutClassIsNotInterface() {
        try {
            HttpConfigClassScanner mock = Mockito.mock(HttpConfigClassScanner.class);
            when(mock.getClasses()).thenReturn(of(TestConfig.class));
            ReactorRestClientFactory restClientFactory = new ReactorRestClientFactory(
                    Mockito.mock(JaxRsClassScanner.class),
                    mock);

            Module module = new AbstractModule() {
                @Override
                protected void configure() {

                    bind(String[].class).annotatedWith(Names.named("args")).toInstance(new String[]{HTTPCONFIG_URL_YML});
                    bind(ServerConfig.class).toInstance(new ServerConfig() {{
                        setEnabled(false);
                    }});

                    ReactorHttpClientProvider httpClientProvider = Mockito.mock(ReactorHttpClientProvider.class);
                    when(httpClientProvider.createClient(any())).thenReturn(Mockito.mock(ReactorHttpClient.class));
                    bind(ReactorHttpClientProvider.class).toInstance(httpClientProvider);
                }
            };
            Guice.createInjector(new AutoBindModules(Modules.override(module).with(restClientFactory)));
            Assert.fail("Should throw exception when class is not an interface");
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("class se.fortnox.reactivewizard.reactorclient.RestClientFactoryTest pointed out in UseInResource annotation must be an interface");
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
