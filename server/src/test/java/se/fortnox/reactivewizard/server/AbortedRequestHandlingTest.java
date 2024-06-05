package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceFactory;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.logging.LoggingShutdownHandler;
import se.fortnox.reactivewizard.test.LoggingMockUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static reactor.core.publisher.Flux.just;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

/**
 * When client aborts the connection the server should still finish gracefully with only a debug-log as the result.
 */
class AbortedRequestHandlingTest {
    @Mock
    private LoggingShutdownHandler loggingShutdownHandler;

    @Test
    void shouldHandleAbortedRequestGracefully() throws InterruptedException {
        LoggingMockUtil.setLevel(ExceptionHandler.class, Level.DEBUG);
        final Appender mockedLogAppender = LoggingMockUtil.createMockedLogAppender(ExceptionHandler.class);

        RwServer rwServer = null;

        CountDownLatch countSuccessfulServerHandlings = new CountDownLatch(1);
        try {
            JaxRsRequestHandler jaxRsRequestHandler = new JaxRsRequestHandler(new Object[]{new TestResourceImpl()}, new JaxRsResourceFactory(), new ExceptionHandler(), false);

            rwServer = server((httpServerRequest, httpServerResponse) -> {
                httpServerRequest.withConnection(connection -> {
                    connection.channel().close();
                });
                return Flux.from(jaxRsRequestHandler.apply(httpServerRequest, httpServerResponse)).doOnComplete(countSuccessfulServerHandlings::countDown);
            });

            HttpClient.create()
                .get()
                .uri("http://localhost:" + rwServer.getServer().port())
                .response()
                .block();
            fail("Should throw exception since client connection is closed");

        } catch (Exception e) {
            verify(mockedLogAppender, timeout(10000)).append(matches(event -> {
                assertThat(event.getMessage().getFormattedMessage()).contains("Inbound connection has been closed: GET /");
            }));

            final boolean await = countSuccessfulServerHandlings.await(10, TimeUnit.SECONDS);
            assertThat(await).isTrue();
        } finally {
            if (rwServer != null) {
                rwServer.getServer().disposeNow();
            }
            LoggingMockUtil.destroyMockedAppender(ExceptionHandler.class);
        }
    }

    private RwServer server(RequestHandler handler) {
        ServerConfig config = new ServerConfig();
        config.setPort(0);
        ConnectionCounter connectionCounter = new ConnectionCounter();
        RequestLogger requestLogger = new RequestLogger();
        CompositeRequestHandler handlers = new CompositeRequestHandler(Collections.singleton(handler), new ExceptionHandler(new ObjectMapper(), requestLogger), connectionCounter, requestLogger);
        return new RwServer(config, handlers, connectionCounter, loggingShutdownHandler);
    }

    @Path("/")
    public class TestResourceImpl {
        @GET
        public Flux<String> get() {
            return just("");
        }
    }
}
