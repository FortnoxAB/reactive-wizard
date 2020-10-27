package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceFactory;
import se.fortnox.reactivewizard.test.LoggingMockUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static rx.Observable.just;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

/**
 * When client aborts the connection the server should still finish gracefully with only a debug-log as the result
 */
public class AbortedRequestHandlingTest {

    @Test
    public void shouldHandleAbortedRequestGracefully() throws NoSuchFieldException, IllegalAccessException {

        final Appender mockedLogAppender = LoggingMockUtil.createMockedLogAppender(ExceptionHandler.class);
        LogManager.getLogger(ExceptionHandler.class).setLevel(Level.DEBUG);

        RwServer      rwServer                   = null;
        AtomicBoolean serverFinishedWithoutError = new AtomicBoolean(false);

        try {
            JaxRsRequestHandler jaxRsRequestHandler = new JaxRsRequestHandler(new Object[]{new TestResourceImpl()}, new JaxRsResourceFactory(), new ExceptionHandler(), false);

            rwServer = server((httpServerRequest, httpServerResponse) -> {
                httpServerRequest.withConnection(connection -> {
                    connection.channel().close();
                });
                return Flux.from(jaxRsRequestHandler.apply(httpServerRequest, httpServerResponse)).doOnComplete(() -> {
                    serverFinishedWithoutError.set(true);
                });
            });

            HttpClient.create()
                .get()
                .uri("http://localhost:" + rwServer.getServer().port())
                .response()
                .block();
            fail("Should throw exception since client connection is closed");

        } catch (Exception e) {
            verify(mockedLogAppender, timeout(3000)).doAppend(matches(event -> {
                assertThat(event.getMessage().toString()).contains("ClosedChannelException: GET /");
            }));
            assertThat(serverFinishedWithoutError.get()).isTrue();
        }
        finally {
            if (rwServer != null) {
                rwServer.getServer().disposeNow();
            }
            LoggingMockUtil.destroyMockedAppender(mockedLogAppender, ExceptionHandler.class);
        }
    }

    private RwServer server(RequestHandler handler) {
        ServerConfig config = new ServerConfig();
        config.setPort(0);
        ConnectionCounter connectionCounter = new ConnectionCounter();
        CompositeRequestHandler handlers = new CompositeRequestHandler(Collections.singleton(handler), new ExceptionHandler(new ObjectMapper()), connectionCounter);
        return new RwServer(config, handlers, connectionCounter);
    }

    @Path("/")
    public class TestResourceImpl {
        @GET
        public Observable<String> get() {
            return just("");
        }
    }
}
