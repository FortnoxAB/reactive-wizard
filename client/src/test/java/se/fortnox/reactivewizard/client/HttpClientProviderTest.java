package se.fortnox.reactivewizard.client;

import com.google.inject.Injector;
import org.junit.jupiter.api.Test;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.server.ServerConfig;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientProviderTest {

    @Test
    void shouldCreateInstanceOfHttpClient() throws URISyntaxException {
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
