package se.fortnox.reactivewizard.client;

import com.google.inject.Injector;
import org.junit.Test;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.server.ServerConfig;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class HttpClientConfigTest {

    @Test
    public void shouldInitializeFromConfig() {
        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});
        }, "src/test/resources/httpconfig.yml");

        HttpClientConfig config = injector.getInstance(HttpClientConfig.class);
        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(443);
        assertThat(config.isHttps()).isFalse();
        assertThat(config.getUrl()).isNull();

        assertThat(config.getMaxConnections()).isEqualTo(1000);
        assertThat(config.getRetryCount()).isEqualTo(15);
        assertThat(config.getRetryDelayMs()).isEqualTo(200);
        assertThat(config.getMaxResponseSize()).isEqualTo(512);

        assertThat(config.getDevCookie()).isEqualTo("TEST=123");
        assertThat(config.getDevServerInfo().getHostString()).isEqualTo("mymachine");
        assertThat(config.getDevServerInfo().getPort()).isEqualTo(9090);
        assertThat(config.getDevHeaders()).contains(entry("Host", "localhost"));
    }

    @Test
    public void shouldInitializeFromConfigWithUrl() {
        Injector injector = TestInjector.create(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});
        }, "src/test/resources/httpconfig_url.yml");

        HttpClientConfig config = injector.getInstance(HttpClientConfig.class);
        assertThat(config.getUrl()).isEqualTo("https://localhost:443/");
        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(443);
        assertThat(config.isHttps()).isTrue();

    }

    @Test
    public void shouldDefaultPortTo80() throws URISyntaxException {
        HttpClientConfig config = new HttpClientConfig("http://localhost");


        assertThat(config.getPort()).isEqualTo(80);
        assertThat(config.isHttps()).isEqualTo(false);
        assertThat(config.getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldSetPortTo443WhenHttps() throws URISyntaxException {
        HttpClientConfig config = new HttpClientConfig("https://localhost");

        assertThat(config.getPort()).isEqualTo(443);
        assertThat(config.isHttps()).isEqualTo(true);
        assertThat(config.getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldSetPort() throws URISyntaxException {
        HttpClientConfig config = new HttpClientConfig("http://localhost:8080");

        assertThat(config.getPort()).isEqualTo(8080);
        assertThat(config.isHttps()).isEqualTo(false);
        assertThat(config.getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldSetHttpsFromProtocolEvenIfPortIsSupplied() throws URISyntaxException {
        HttpClientConfig config = new HttpClientConfig("https://localhost:8080");

        assertThat(config.getPort()).isEqualTo(8080);
        assertThat(config.isHttps()).isEqualTo(true);
        assertThat(config.getHost()).isEqualTo("localhost");
    }


    @Test
    public void shouldAddHttpWhenProtocolIsMissing() throws URISyntaxException {
        HttpClientConfig config = new HttpClientConfig("localhost");

        assertThat(config.getPort()).isEqualTo(80);
        assertThat(config.isHttps()).isEqualTo(false);
        assertThat(config.getUrl()).startsWith("http://");
        assertThat(config.getHost()).isEqualTo("localhost");
    }

    @Test
    public void shouldBlockStartupIfHostCannotBeResolved() {
        String host = "Nonexistinghost" + new Date().getTime();
        try {
            new HttpClientConfig(host);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).isEqualTo("Cannot resolve host for httpClient: " + host);
            assertThat(e.getCause()).isInstanceOf(UnknownHostException.class);
        }
    }

    @Test
    public void shouldNotBeInsecureByDefault() throws URISyntaxException {
        HttpClientConfig httpClientConfig = new HttpClientConfig("https://example.com");
        assertThat(httpClientConfig.isHttps()).isTrue();
        assertThat(httpClientConfig.isValidateCertificates()).isTrue();
    }
}
