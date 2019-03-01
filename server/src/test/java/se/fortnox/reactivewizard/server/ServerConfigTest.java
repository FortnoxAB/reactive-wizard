package se.fortnox.reactivewizard.server;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ServerConfigTest {

    private final ServerConfig serverConfig = new ServerConfig();

    @Test
    public void shouldSetCorrectDefaultValues() {
        assertThat(serverConfig.isEnabled()).isTrue();
        assertThat(serverConfig.getPort()).isEqualTo(8080);
        assertThat(serverConfig.getMaxHeaderSize()).isEqualTo(20480);
        assertThat(serverConfig.getShutdownTimeoutSeconds()).isEqualTo(20);
        assertThat(serverConfig.getMaxInitialLineLengthDefault()).isEqualTo(4096);
        assertThat(serverConfig.getMaxRequestSize()).isEqualTo(10*1024*1024);
    }

    @Test
    public void changesToTheConfigShouldBePersisted() {
        serverConfig.setPort(1337);
        serverConfig.setEnabled(false);
        serverConfig.setMaxHeaderSize(123);
        serverConfig.setShutdownTimeoutMs(4344);
        serverConfig.setMaxInitialLineLengthDefault(1344);
        serverConfig.setMaxRequestSize(314159);

        assertThat(serverConfig.isEnabled()).isFalse();
        assertThat(serverConfig.getPort()).isEqualTo(1337);
        assertThat(serverConfig.getMaxHeaderSize()).isEqualTo(123);
        assertThat(serverConfig.getShutdownTimeoutSeconds()).isEqualTo(4344);
        assertThat(serverConfig.getMaxInitialLineLengthDefault()).isEqualTo(1344);
        assertThat(serverConfig.getMaxRequestSize()).isEqualTo(314159);
    }
}
