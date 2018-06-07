package se.fortnox.reactivewizard.client;

import com.google.inject.Injector;
import org.junit.Test;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.server.ServerConfig;

import java.net.URISyntaxException;

import static org.fest.assertions.Assertions.assertThat;

public class HttpClientProviderTest {

    @Test
    public void shouldCreateInstanceOfHttpClient() throws URISyntaxException {
        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig(){{
                setEnabled(false);
            }});
        });
        HttpClientProvider provider = injector.getInstance(HttpClientProvider.class);

        HttpClientConfig httpClientConfig = new HttpClientConfig("localhost:8080");
        HttpClient       client           = provider.createClient(httpClientConfig);

        assertThat(client).isNotNull();
        assertThat(client).isInstanceOf(HttpClient.class);
    }

}
