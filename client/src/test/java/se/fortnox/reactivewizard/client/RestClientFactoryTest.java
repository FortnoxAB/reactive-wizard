package se.fortnox.reactivewizard.client;

import com.google.inject.Binder;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import se.fortnox.reactivewizard.UseHttpClientConfig;
import se.fortnox.reactivewizard.binding.scanners.JaxRsClassScanner;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.server.ServerConfig;
import se.fortnox.reactivewizard.validation.ValidationFailedException;

import javax.ws.rs.GET;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.collect.ImmutableSet.of;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RestClientFactoryTest {

    private static final String SRC_TEST_RESOURCES = "src/test/resources/";
    private static final String HTTPCONFIG_URL_YML = SRC_TEST_RESOURCES + "httpconfig_url.yml";
    private static final String HTTPCONFIG_YML     = SRC_TEST_RESOURCES + "httpconfig.yml";

    @Test
    public void shouldProxyResourceInterfacesToHttpClient() throws URISyntaxException {

        AtomicReference<HttpClient> httpClientSpy = new AtomicReference<>();

        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});

            HttpClientProvider httpClientProvider = Mockito.mock(HttpClientProvider.class);
            mockResourcesOnClassPath(binder, of(TestResource.class));

            when(httpClientProvider.createClient(any(HttpClientConfig.class))).thenAnswer(invocation -> {
                HttpClientConfig httpClientConfig = invocation.getArgumentAt(0, HttpClientConfig.class);
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
    public void shouldUseCustomHttpClientConfigWhenRequested() throws URISyntaxException {
        AtomicReference<HttpClient> mockClient = new AtomicReference<>(Mockito.mock(HttpClient.class));
        AtomicReference<HttpClient> mockCustomClient = new AtomicReference<>();

        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});

            mockResourcesOnClassPath(binder, of(TestResource.class, CustomTestResource.class, CustomTestNoConfigValueResource.class));

            HttpClientProvider httpClientProvider = Mockito.mock(HttpClientProvider.class);

            when(httpClientProvider.createClient(any())).thenAnswer(invocation -> {
                HttpClientConfig httpClientConfig = invocation.getArgumentAt(0, HttpClientConfig.class);
                HttpClient httpClient = Mockito.spy(new HttpClient(httpClientConfig));
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

        verifyZeroInteractions(mockClient.get());

        //Resetting the custom mock to be able
        reset(mockCustomClient.get());

        TestResource testResource = injector.getInstance(TestResource.class);
        verify(mockClient.get(), times(1)).create(any());

        testResource.testCall();
        verify(mockClient.get(), times(1)).invoke(any(), any(), any());
        verifyZeroInteractions(mockCustomClient.get());

        reset(mockClient.get());

        CustomTestNoConfigValueResource testResourceNoConfigValue = injector.getInstance(CustomTestNoConfigValueResource.class);
        verify(mockClient.get(), times(1)).create(any());

        testResourceNoConfigValue.testCall();
        verify(mockClient.get(), times(1)).invoke(any(), any(), any());
        verifyZeroInteractions(mockCustomClient.get());
    }

    private void mockResourcesOnClassPath(Binder binder, Set<Class<?>> classes) {
        JaxRsClassScanner mock = Mockito.mock(JaxRsClassScanner.class);
        when(mock.getClasses()).thenReturn(classes);
        binder.bind(JaxRsClassScanner.class).toInstance(mock);
    }

    @Test
    public void testThatStartupFailsWhenUseHttpClientConfigPointsToNonHttpClientConfig() {
        try {
            TestInjector.create(binder -> {
                binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                    setEnabled(false);
                }});
                mockResourcesOnClassPath(binder, of(TestResource.class, CustomTestResource.class, CustomTestNoConfigValueResource.class, CustomDatabaseConfigValueResource.class));
            }, HTTPCONFIG_URL_YML);
            Assert.fail("Should throw exception");
        } catch (CreationException creationException) {
            assertThat(creationException.getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(creationException.getCause().getMessage()).isEqualTo("Illegal argument in UseHttpClientConfig annotation. Configpath test is of class se.fortnox.reactivewizard.client.TestConfig but must be subclass of HttpClientConfig");
        }
    }

    @Test
    public void testThatStartupFailsWhenUseHttpClientConfigPointsToNonExistingYmlConfig() {
        try {
            TestInjector.create(binder -> {
                binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                    setEnabled(false);
                }});
                mockResourcesOnClassPath(binder, of(TestResource.class, CustomTestResource.class));
            }, HTTPCONFIG_YML).getInstance(CustomTestResource.class);

            Assert.fail("Should throw exception");
        } catch (ProvisionException provisionException) {
            assertThat(provisionException.getCause()).isInstanceOf(ValidationFailedException.class);
            assertThat(((ValidationFailedException)provisionException.getCause()).getFields()[0].getField()).isEqualTo("host");
            assertThat(((ValidationFailedException)provisionException.getCause()).getFields()[0].getError()).isEqualTo("validation.notnull");
        }
    }

    @Test
    public void testThatStartupFailsWhenUseHttpClientConfigPointsToNonExistingConfigClass() {
        try {
            TestInjector.create(binder -> {
                binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                    setEnabled(false);
                }});
                mockResourcesOnClassPath(binder, of(TestResource.class, CustomTestResource.class, CustomTestNoConfigValueResource.class, CustomNotFoundConfigValueResource.class));
            }, HTTPCONFIG_YML);
            Assert.fail("Should throw exception");
        } catch (CreationException creationException) {
            assertThat(creationException.getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(creationException.getCause().getMessage()).isEqualTo("No config class found for config not_found");
        }
    }
}

interface TestResource {
    @GET
    Observable<String> testCall();
}

@UseHttpClientConfig("customHttpClient")
interface CustomTestResource {
    @GET
    Observable<String> testCall();
}

@UseHttpClientConfig
interface CustomTestNoConfigValueResource {
    @GET
    Observable<String> testCall();
}

@UseHttpClientConfig("test")
interface CustomDatabaseConfigValueResource {
    @GET
    Observable<String> testCall();
}

@UseHttpClientConfig("not_found")
interface CustomNotFoundConfigValueResource {
    @GET
    Observable<String> testCall();
}


