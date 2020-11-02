package se.fortnox.reactivewizard.server;

import io.netty.channel.EventLoopGroup;
import org.apache.log4j.Appender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpServer;
import se.fortnox.reactivewizard.test.LoggingMockUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static reactor.core.publisher.Mono.just;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

@RunWith(MockitoJUnitRunner.class)
public class RwServerTest {

    @Mock
    ConnectionCounter connectionCounter;

    @Mock
    CompositeRequestHandler compositeRequestHandler;

    @Before
    public void before() {
        RwServer.registerShutdownDependency(null);
    }

    @Test
    public void shouldSetServerToNullIfConfigSaysDisabled() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setEnabled(false);
        serverConfig.setPort(0);

        RwServer rwServer = new RwServer(serverConfig, compositeRequestHandler, connectionCounter, null);
        rwServer.join();

        assertNull(rwServer.getServer());
    }

    @Test
    public void shouldStartTheServerIfConfigSaysEnabled() throws InterruptedException {
        ServerConfig  serverConfig              = new ServerConfig();
        serverConfig.setPort(0);
        AtomicInteger startInvocedNumberOfTimes = new AtomicInteger(0);

        RwServer rwServer = null;
        try {
            rwServer = new RwServer(serverConfig, compositeRequestHandler, connectionCounter, null) {
                @Override
                public void start() {
                    startInvocedNumberOfTimes.incrementAndGet();
                }
            };
            rwServer.join();

            assertThat(rwServer.getServer()).isNotNull().isInstanceOf(DisposableServer.class);
            assertThat(startInvocedNumberOfTimes.get()).isEqualTo(1);
        } finally {
            if (rwServer != null) {
                rwServer.getServer().disposeNow();
            }
        }
    }

    @Test
    public void shouldAwaitShutDown() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();

        DisposableServer disposableServer = mock(DisposableServer.class);
        when(disposableServer.onDispose()).thenReturn(Mono.empty());
        HttpServer server = new HttpServer() {
            @Override
            protected Mono<? extends DisposableServer> bind(TcpServer b) {
                return just(disposableServer);
            }
        };
        RwServer rwServer = new RwServer(serverConfig, connectionCounter, server, compositeRequestHandler, null) {};
        rwServer.join();

        verify(disposableServer, times(1)).onDispose();
    }

    @Test
    public void shouldLogThatShutDownIsRegistered() {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RwServer.class);

        LoopResources loopResources = mock(LoopResources.class);
        when(loopResources.disposeLater(any(), any())).thenReturn(Mono.empty());

        RwServer rwServer = null;
        try {
            final ServerConfig config = new ServerConfig();
            config.setPort(0);
            rwServer = new RwServer(config, compositeRequestHandler, connectionCounter, null) {
            };
            RwServer.shutdownHook(config, rwServer.getServer(), loopResources, connectionCounter);

            verify(mockAppender).doAppend(matches(log ->
                assertThat(log.getMessage().toString()).matches("Shutdown requested. Will wait up to 20 seconds...")
            ));

            verify(mockAppender).doAppend(matches(log ->
                assertThat(log.getMessage().toString()).matches("Shutdown complete")
            ));

            LoggingMockUtil.destroyMockedAppender(mockAppender, RwServer.class);
        } finally {
            if (rwServer != null) {
                rwServer.getServer().disposeNow();
            }
        }
    }

    @Test
    public void shouldCallServerShutDownWhenShutdownHookIsInvoked() {
        LoopResources loopResources = mock(LoopResources.class);
        when(loopResources.disposeLater(any(), any())).thenReturn(Mono.empty());

        DisposableServer disposableServer = mock(DisposableServer.class);
        when(disposableServer.onDispose()).thenReturn(Mono.empty());
        HttpServer server = new HttpServer() {
            @Override
            protected Mono<? extends DisposableServer> bind(TcpServer b) {
                return just(disposableServer);
            }
        };

        RwServer rwServer = new RwServer(new ServerConfig(), connectionCounter, server, compositeRequestHandler, loopResources) {};
        RwServer.shutdownHook(new ServerConfig(), rwServer.getServer(), loopResources, connectionCounter);

        verify(loopResources, times(1)).disposeLater(any(), any());
        verify(disposableServer, times(1)).disposeNow(any());
    }

    @Test
    public void shouldLogErrorIfShutdownIsPerformedWhileConnectionCountIsNotZero() {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RwServer.class);

        LoopResources loopResources = mock(LoopResources.class);
        when(loopResources.disposeLater(any(), any())).thenReturn(Mono.empty());

        when(connectionCounter.awaitZero(anyInt(), any(TimeUnit.class))).thenReturn(false);
        when(connectionCounter.getCount()).thenReturn(4L);
        RwServer rwServer = null;
        try {
            final ServerConfig config = new ServerConfig();
            config.setPort(0);
            rwServer = new RwServer(config, compositeRequestHandler, connectionCounter, null) {
            };
            RwServer.shutdownHook(config, rwServer.getServer(), loopResources, connectionCounter);

            verify(mockAppender).doAppend(matches(log ->
                assertThat(log.getMessage().toString()).matches("Shutdown requested. Will wait up to 20 seconds...")
            ));

            verify(mockAppender).doAppend(matches(log ->
                assertThat(log.getMessage().toString()).matches("Shutdown proceeded while connection count was not zero: 4")
            ));

            LoggingMockUtil.destroyMockedAppender(mockAppender, RwServer.class);
        } finally {
            rwServer.getServer().disposeNow();
        }
    }

    @Test
    public void shouldNotOverrideShutdownDependency() {
        RwServer.registerShutdownDependency(() -> {});
        try {
            RwServer.registerShutdownDependency(() -> {});
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("Shutdown dependency is already registered");
        }
    }

    @Test
    public void shouldSkipAwaitingShutdownDependencyIfNotSet() {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RwServer.class);
        RwServer.awaitShutdownDependency(new ServerConfig().getShutdownTimeoutSeconds());
        verify(mockAppender, never()).doAppend(any());
    }

    @Test
    public void shouldAwaitShutdownDependency() {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RwServer.class);
        Supplier supplier = mock(Supplier.class);

        RwServer.registerShutdownDependency(supplier::get);
        verify(supplier, never()).get();

        RwServer.awaitShutdownDependency(new ServerConfig().getShutdownTimeoutSeconds());

        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Wait for completion of shutdown dependency")
        ));
        verify(supplier, times(1)).get();
        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Shutdown dependency completed, continue...")
        ));
    }
}
