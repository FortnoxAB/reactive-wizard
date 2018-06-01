package se.fortnox.reactivewizard.client;

import com.google.inject.Injector;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.server.ServerConfig;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RestClientFactoryTest {

    @Test
    public void shouldProxyResourceInterfacesToHttpClient() throws URISyntaxException {
        HttpClientConfig httpClientConfig = new HttpClientConfig();
        httpClientConfig.setUrl("http://anything");

        HttpClient mockClient = Mockito.spy(new HttpClient(httpClientConfig));

        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});
            binder.bind(HttpClient.class).toInstance(mockClient);
        });

        // Verify that it creates a proxy for the resource
        TestResource testResource = injector.getInstance(TestResource.class);
        verify(mockClient, times(1)).create(any());

        // Verify that calls to the resource pass through the HttpClient
        verify(mockClient, never()).invoke(any(), any(), any());
        testResource.testCall();
        verify(mockClient, times(1)).invoke(any(), any(), any());
    }
}

@Path("/test")
interface TestResource {
    @GET
    Observable<String> testCall();
}

