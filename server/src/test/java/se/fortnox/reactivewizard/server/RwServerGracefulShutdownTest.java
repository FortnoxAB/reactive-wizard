package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RwServerGracefulShutdownTest {

    private ServerConfig serverConfig;

    @Before
    public void before() {
        serverConfig = new ServerConfig();
        serverConfig.setPort(0);
    }

    @Test
    public void shouldThrowExceptionIfGracefulShutDownIsNotPossible() throws InterruptedException {
        CountDownLatch serverReceivedClientRequest = new CountDownLatch(1);
        CountDownLatch clientReceivedResponse = new CountDownLatch(1);
        serverConfig.setShutdownTimeoutMs(1);

        // start server with a endpoint that takes 30 seconds to respond.
        Duration longResponseTime = Duration.ofSeconds(30);
        RwServer rwServer = server(withEndpoint(serverReceivedClientRequest, longResponseTime));

        // client connects to endpoint and waits for server to respond.
        clientSendsRequestToSlowEndpoint(rwServer,clientReceivedResponse);

        assertThat(serverReceivedClientRequest.await(3, TimeUnit.SECONDS))
            .describedAs("server should receive the client request")
            .isTrue();

        try {
            // shutdown of server starts. A graceful shutdown is attempted.
            RwServer.shutdownHook(serverConfig, rwServer.getServer(), null, new ConnectionCounter());
            fail("Server should throw an exception that graceful shutdown could not be done. Expected IllegalStateException");
        } catch (IllegalStateException illegalStateException) {
                assertThat(illegalStateException.getMessage())
                    .describedAs("server should throw an exception that graceful shutdown could not be done. The connection was cut.")
                    .isEqualTo("Socket couldn't be stopped within 1000ms");
        }
    }

    @Test
    public void shouldGracefullyShutDown() throws InterruptedException {
        CountDownLatch serverReceivedClientRequest = new CountDownLatch(1);
        CountDownLatch clientReceivedResponse = new CountDownLatch(1);
        serverConfig.setShutdownTimeoutMs(20);

        // start server with a endpoint that takes 1 seconds to respond.
        Duration responseTime = Duration.ofSeconds(1);
        RwServer rwServer = server(withEndpoint(serverReceivedClientRequest, responseTime));

        // client connects to endpoint and waits for server to respond.
        clientSendsRequestToSlowEndpoint(rwServer,clientReceivedResponse);

        assertThat(serverReceivedClientRequest.await(3, TimeUnit.SECONDS))
            .describedAs("server should receive the client request")
            .isTrue();

        // shutdown of server starts. A graceful shutdown is attempted.
        RwServer.shutdownHook(serverConfig, rwServer.getServer(), null, new ConnectionCounter());

        // server waits for connection to complete before termination.
        assertThat(clientReceivedResponse.await(5, TimeUnit.SECONDS))
            .describedAs("client should get response")
            .isTrue();

        assertThat(rwServer.getServer().isDisposed())
            .describedAs("server should shutdown")
            .isTrue();
    }

    private RequestHandler withEndpoint(CountDownLatch connectionReceived, Duration responseTime) {
        return (httpServerRequest, httpServerResponse) -> {
            connectionReceived.countDown();
            return httpServerResponse
                .chunkedTransfer(false)
                .sendByteArray(Mono.just("Message that we want to receive before server terminates".getBytes())
                    .delayElement(responseTime));
        };
    }

    private void clientSendsRequestToSlowEndpoint(RwServer rwServer, CountDownLatch responseReceived) {
        HttpClient.ResponseReceiver<?> responseReceiver = HttpClient.create()
            .baseUrl("http://localhost:" + rwServer.getServer().port())
            .get();

        responseReceiver.responseContent()
            .aggregate()
            .asString()
            .doOnNext(e -> {
                responseReceived.countDown();
            }).subscribe();
    }


    private RwServer server(RequestHandler handler) {
        ConnectionCounter       connectionCounter = new ConnectionCounter();
        CompositeRequestHandler handlers          = new CompositeRequestHandler(Collections.singleton(handler), new ExceptionHandler(new ObjectMapper()), connectionCounter);
        return new RwServer(serverConfig, handlers, connectionCounter) {
            @Override
            void registerShutdownHook() {
                // NOOP implementation , so that we dont do a shutdown twice.
            }
        };
    }
}
