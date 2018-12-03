package se.fortnox.reactivewizard.server;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.apache.log4j.Appender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.fortnox.reactivewizard.test.LoggingMockUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

@RunWith(MockitoJUnitRunner.class)
public class RxNettyServerTest {

    @Mock
    HttpServer<ByteBuf, ByteBuf> server;

    @Mock
    ConnectionCounter connectionCounter;

    @Mock
    CompositeRequestHandler compositeRequestHandler;

    @Before
    public void before() {
        when(server.start(compositeRequestHandler)).thenReturn(server);
    }

    @Test
    public void shouldSetServerToNullIfConfigSaysDisabled() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setEnabled(false);

        RxNettyServer rxNettyServer = new RxNettyServer(serverConfig, connectionCounter, server, compositeRequestHandler);
        rxNettyServer.join();

        assertNull(rxNettyServer.getServer());
    }

    @Test
    public void shouldStartTheServerIfConfigSaysEnabled() throws InterruptedException {
        ServerConfig  serverConfig              = new ServerConfig();
        AtomicInteger startInvocedNumberOfTimes = new AtomicInteger(0);

        RxNettyServer rxNettyServer = new RxNettyServer(serverConfig, connectionCounter, server, compositeRequestHandler) {
            @Override
            public void start() {
                startInvocedNumberOfTimes.incrementAndGet();
            }
        };
        rxNettyServer.join();

        assertThat(rxNettyServer.getServer()).isEqualTo(server);
        assertThat(startInvocedNumberOfTimes.get()).isEqualTo(1);
    }

    @Test
    public void shouldAwaitShutDown() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();

        RxNettyServer rxNettyServer = new RxNettyServer(serverConfig, connectionCounter, server, compositeRequestHandler) {};
        rxNettyServer.join();

        verify(server, times(1)).awaitShutdown();
    }

    @Test
    public void shouldLogThatShutDownIsRegistered() throws NoSuchFieldException, IllegalAccessException {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RxNettyServer.class);

        RxNettyServer.shutdownHook(new ServerConfig(), server, connectionCounter);

        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Shutdown requested. Will wait up to 20 seconds...")
        ));

        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Shutdown complete")
        ));

        LoggingMockUtil.destroyMockedAppender(mockAppender, RxNettyServer.class);
    }

    @Test
    public void shouldCallServerShutDownWhenShutdownHookIsInvoked() {
        RxNettyServer.shutdownHook(new ServerConfig(), server, connectionCounter);

        verify(server, times(1)).shutdown();
        verify(server, times(1)).awaitShutdown();
    }

    @Test
    public void shouldLogErrorIfShutdownIsPerformedWhileConnectionCountIsNotZero() throws NoSuchFieldException, IllegalAccessException {
        Appender mockAppender = LoggingMockUtil.createMockedLogAppender(RxNettyServer.class);

        when(connectionCounter.awaitZero(anyInt(), any(TimeUnit.class))).thenReturn(false);
        when(connectionCounter.getCount()).thenReturn(4L);

        RxNettyServer.shutdownHook(new ServerConfig(), server, connectionCounter);

        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Shutdown requested. Will wait up to 20 seconds...")
        ));

        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Shutdown proceeded while connection count was not zero: 4")
        ));

        LoggingMockUtil.destroyMockedAppender(mockAppender, RxNettyServer.class);
    }

}
