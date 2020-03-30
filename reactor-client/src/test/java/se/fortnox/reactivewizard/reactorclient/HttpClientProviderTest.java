package se.fortnox.reactivewizard.reactorclient;

import com.google.inject.Injector;
import org.junit.Test;
import se.fortnox.reactivewizard.client.HttpClientConfig;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.server.ServerConfig;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpClientProviderTest {

    @Test
    public void shouldCreateInstanceOfHttpClient() throws URISyntaxException {
        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig(){{
                setEnabled(false);
            }});
        });
        ReactorHttpClientProvider provider = injector.getInstance(ReactorHttpClientProvider.class);

        HttpClientConfig httpClientConfig = new HttpClientConfig("localhost:8080");
        ReactorHttpClient       client           = provider.createClient(httpClientConfig);

        assertThat(client).isNotNull();
        assertThat(client).isInstanceOf(ReactorHttpClient.class);
    }

}
