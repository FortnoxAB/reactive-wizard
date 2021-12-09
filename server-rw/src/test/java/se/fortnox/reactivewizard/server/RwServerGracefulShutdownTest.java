package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

public class RwServerGracefulShutdownTest {

    private ServerConfig serverConfig;
    private AtomicLong requests;
    private AtomicLong responses;

    @Before
    public void before() {
        serverConfig = new ServerConfig();
        serverConfig.setPort(0);
        requests = new AtomicLong(0);
        responses = new AtomicLong(0);
    }

    @Test
    public void shouldThrowExceptionIfGracefulShutDownIsNotPossible() {
        serverConfig.setShutdownTimeoutMs(1);

        // start server with a endpoint that takes 30 seconds to respond.
        Duration longResponseTime = Duration.ofSeconds(30);
        RwServer rwServer = server(withEndpoint(longResponseTime));

        // client connects to endpoint and waits for server to respond.
        clientSendsRequestToSlowEndpoint(rwServer);

        await()
            .atMost(Duration.ofSeconds(3))
            .until(() -> requests.get() == 1);

        DisposableServer server = rwServer.getServer();
        ConnectionCounter connectionCounter = new ConnectionCounter();

        assertThatExceptionOfType(IllegalStateException.class)
            .describedAs("server should throw an exception that graceful shutdown could not be done. The connection was cut.")
            .isThrownBy(() -> RwServer.shutdownHook(serverConfig, server, connectionCounter))
            .withMessage("Socket couldn't be stopped within 1000ms");
    }


    @Test
    public void shouldWaitForShutdownDependencyBeforeDisposingServer() {
        try {
            AtomicBoolean hasBeenRun = new AtomicBoolean(false);
            //Block shutdown for 3 seconds
            RwServer.registerShutdownDependency(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                        hasBeenRun.set(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            RwServer rwServer = server(withEndpoint(Duration.ofSeconds(1)));
            Thread shutdown = new Thread(() -> RwServer.shutdownHook(serverConfig, rwServer.getServer(), new ConnectionCounter()));
            shutdown.start();
            await("Server should not be disposed until after five seconds")
                .atLeast(Duration.ofMillis(4500))
                .until(() -> rwServer.getServer().isDisposed());
            assertThat(hasBeenRun.get())
                .isTrue();
        } finally {
            RwServer.registerShutdownDependency(null);
        }
    }

    @Test
    public void shouldWaitFiveSecondsAsDefaultBeforeDisposingServer() {
        RwServer rwServer = server(withEndpoint(Duration.ofSeconds(1)));
        Thread shutdown = new Thread(() -> RwServer.shutdownHook(serverConfig, rwServer.getServer(), new ConnectionCounter()));
        shutdown.start();
        await("Server should not be disposed until after five seconds")
            .atLeast(Duration.ofMillis(4500))
            .until(() -> rwServer.getServer().isDisposed());
    }

    @Test
    public void shouldWaitSpecifiedNumberOfSecondsBeforeDisposingServer() {
        serverConfig.setShutdownDelaySeconds(1);
        RwServer rwServer = server(withEndpoint(Duration.ofSeconds(1)));
        Thread shutdown = new Thread(() -> RwServer.shutdownHook(serverConfig, rwServer.getServer(), new ConnectionCounter()));
        shutdown.start();
        await("Server should be disposed in two seconds")
            .atMost(2, SECONDS)
            .until(() -> rwServer.getServer().isDisposed());
    }

    private RequestHandler withEndpoint(Duration responseTime) {
        return (httpServerRequest, httpServerResponse) -> {
            requests.incrementAndGet();
            return httpServerResponse
                .chunkedTransfer(false)
                .sendByteArray(Mono.just("Message that we want to receive before server terminates".getBytes())
                    .delayElement(responseTime));
        };
    }

    private void clientSendsRequestToSlowEndpoint(RwServer rwServer) {
        HttpClient.ResponseReceiver<?> responseReceiver = HttpClient.create()
            .baseUrl("http://localhost:" + rwServer.getServer().port())
            .get();

        responseReceiver.responseContent()
            .aggregate()
            .asString()
            .doOnNext(e -> {
                responses.incrementAndGet();
            }).subscribe();
    }

    private RwServer server(RequestHandler handler) {
        ConnectionCounter connectionCounter = new ConnectionCounter();
        CompositeRequestHandler handlers = new CompositeRequestHandler(Collections.singleton(handler), new ExceptionHandler(new ObjectMapper()), connectionCounter);
        return new RwServer(serverConfig, handlers, connectionCounter) {
            @Override
            void registerShutdownHook() {
                // NOOP implementation , so that we dont do a shutdown twice.
            }
        };
    }
}