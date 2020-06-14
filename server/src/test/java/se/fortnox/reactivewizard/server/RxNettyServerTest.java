package se.fortnox.reactivewizard.server;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.log4j.Appender;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.test.LoggingMockUtil;

import java.net.MalformedURLException;
import java.net.URL;
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
public class RxNettyServerTest {

    HttpServer server;

    @Mock
    LoopResources loopResources;

    @Mock
    ConnectionCounter connectionCounter;

    @Mock
    CompositeRequestHandler compositeRequestHandler;

    @Before
    public void before() {
        RxNettyServer.registerShutdownDependency(null);
        //when(server.handle(compositeRequestHandler)).thenReturn(server);
        when(loopResources.disposeLater(any(), any())).thenReturn(Mono.empty());
    }

    @Test
    public void shouldSetServerToNullIfConfigSaysDisabled() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setEnabled(false);

        HttpServer server = HttpServer.create().port(0);
        RxNettyServer rxNettyServer = new RxNettyServer(serverConfig, connectionCounter, server, compositeRequestHandler, loopResources);
        rxNettyServer.join();

        assertNull(rxNettyServer.getServer());
    }

    @Test
    public void shouldStartTheServerIfConfigSaysEnabled() throws InterruptedException {
        ServerConfig  serverConfig              = new ServerConfig();
        AtomicInteger startInvocedNumberOfTimes = new AtomicInteger(0);

        RxNettyServer rxNettyServer = null;
        try {
            server = HttpServer.create().port(0);
            rxNettyServer = new RxNettyServer(serverConfig, connectionCounter, server, compositeRequestHandler, loopResources) {
                @Override
                public void start() {
                    startInvocedNumberOfTimes.incrementAndGet();
                }
            };
            rxNettyServer.join();

            assertThat(rxNettyServer.getServer()).isNotNull().isInstanceOf(DisposableServer.class);
            assertThat(startInvocedNumberOfTimes.get()).isEqualTo(1);
        } finally {
            if (rxNettyServer != null) {
                rxNettyServer.getServer().disposeNow();
            }
        }
    }

    @Test
    public void shouldAwaitShutDown() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();

        DisposableServer disposableServer = mock(DisposableServer.class);
        when(disposableServer.onDispose()).thenReturn(Mono.empty());
        server = new HttpServer() {
            @Override
            protected Mono<? extends DisposableServer> bind(TcpServer b) {
                return just(disposableServer);
            }
        };
        RxNettyServer rxNettyServer = new RxNettyServer(serverConfig, connectionCounter, server, compositeRequestHandler, loopResources) {};
        rxNettyServer.join();

        verify(disposableServer, times(1)).onDispose();
    }

    @Test
    public void shouldLogThatShutDownIsRegistered() throws NoSuchFieldException, IllegalAccessException {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RxNettyServer.class);

        RxNettyServer rxNettyServer = null;
        try {
            server = HttpServer.create().port(0);
            rxNettyServer = new RxNettyServer(new ServerConfig(), connectionCounter, server, compositeRequestHandler, loopResources) {
            };
            RxNettyServer.shutdownHook(new ServerConfig(), rxNettyServer.getServer(), loopResources, connectionCounter);

            verify(mockAppender).doAppend(matches(log ->
                assertThat(log.getMessage().toString()).matches("Shutdown requested. Will wait up to 20 seconds...")
            ));

            verify(mockAppender).doAppend(matches(log ->
                assertThat(log.getMessage().toString()).matches("Shutdown complete")
            ));

            LoggingMockUtil.destroyMockedAppender(mockAppender, RxNettyServer.class);
        } finally {
            rxNettyServer.getServer().disposeNow();
        }
    }

    @Test
    public void shouldCallServerShutDownWhenShutdownHookIsInvoked() {
        DisposableServer disposableServer = mock(DisposableServer.class);
        when(disposableServer.onDispose()).thenReturn(Mono.empty());
        server = new HttpServer() {
            @Override
            protected Mono<? extends DisposableServer> bind(TcpServer b) {
                return just(disposableServer);
            }
        };

        RxNettyServer rxNettyServer = new RxNettyServer(new ServerConfig(), connectionCounter, server, compositeRequestHandler, loopResources) {};
        RxNettyServer.shutdownHook(new ServerConfig(), rxNettyServer.getServer(), loopResources, connectionCounter);

        verify(loopResources, times(1)).disposeLater(any(), any());
        verify(disposableServer, times(1)).disposeNow(any());
    }

    @Test
    public void shouldLogErrorIfShutdownIsPerformedWhileConnectionCountIsNotZero() throws NoSuchFieldException, IllegalAccessException {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RxNettyServer.class);

        when(connectionCounter.awaitZero(anyInt(), any(TimeUnit.class))).thenReturn(false);
        when(connectionCounter.getCount()).thenReturn(4L);
        RxNettyServer rxNettyServer = null;
        try {
            server = HttpServer.create().port(0);
            rxNettyServer = new RxNettyServer(new ServerConfig(), connectionCounter, server, compositeRequestHandler, loopResources) {
            };
            RxNettyServer.shutdownHook(new ServerConfig(), rxNettyServer.getServer(), loopResources, connectionCounter);

            verify(mockAppender).doAppend(matches(log ->
                assertThat(log.getMessage().toString()).matches("Shutdown requested. Will wait up to 20 seconds...")
            ));

            verify(mockAppender).doAppend(matches(log ->
                assertThat(log.getMessage().toString()).matches("Shutdown proceeded while connection count was not zero: 4")
            ));

            LoggingMockUtil.destroyMockedAppender(mockAppender, RxNettyServer.class);
        } finally {
            rxNettyServer.getServer().disposeNow();
        }
    }

    @Test
    public void shouldNotOverrideShutdownDependency() {
        RxNettyServer.registerShutdownDependency(() -> {});
        try {
            RxNettyServer.registerShutdownDependency(() -> {});
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("Shutdown dependency is already registered");
        }
    }

    @Test
    public void shouldSkipAwaitingShutdownDependencyIfNotSet() throws NoSuchFieldException, IllegalAccessException {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RxNettyServer.class);
        RxNettyServer.awaitShutdownDependency(new ServerConfig().getShutdownTimeoutSeconds());
        verify(mockAppender, never()).doAppend(any());
    }

    @Test
    public void shouldAwaitShutdownDependency() throws NoSuchFieldException, IllegalAccessException {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RxNettyServer.class);
        Supplier supplier = mock(Supplier.class);

        RxNettyServer.registerShutdownDependency(supplier::get);
        verify(supplier, never()).get();

        RxNettyServer.awaitShutdownDependency(new ServerConfig().getShutdownTimeoutSeconds());

        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Wait for completion of shutdown dependency")
        ));
        verify(supplier, times(1)).get();
        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Shutdown dependency completed, continue...")
        ));
    }
}
