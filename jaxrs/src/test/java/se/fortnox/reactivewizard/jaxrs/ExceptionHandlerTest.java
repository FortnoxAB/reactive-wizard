package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Test;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import java.nio.channels.ClosedChannelException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

public class ExceptionHandlerTest {

    @Test
    public void shouldLogHeadersForErrors() {
        MockHttpServerRequest request = new MockHttpServerRequest("/path");
        request.addHeader("X-DBID", "5678");
        String expectedLog = "500 Internal Server Error\n\tCause: runtime exception\n" +
            "\tResponse: {\"id\":\"*\",\"error\":\"internal\"}\n\tRequest: GET /path headers: X-DBID=5678 ";
        assertLog(request, new RuntimeException("runtime exception"), Level.ERROR, expectedLog);
    }

    @Test
    public void shouldLogHeadersForWarnings() {
        MockHttpServerRequest request = new MockHttpServerRequest("/path");
        request.addHeader("X-DBID", "5678");
        assertLog(request,
            new WebException(HttpResponseStatus.BAD_REQUEST),
            Level.WARN,
            "400 Bad Request\n\tCause: -\n\tResponse: {\"id\":\"*\",\"error\":\"badrequest\"}\n\tRequest: GET /path headers: X-DBID=5678 ");
    }

    @Test
    public void shouldLog404AsDebug() {
        MockHttpServerRequest request = new MockHttpServerRequest("/path");
        String expectedLog = "404 Not Found\n" +
            "\tCause: -\n" +
            "\tResponse: {\"id\":\"*\",\"error\":\"notfound\"}\n" +
            "\tRequest: GET /path headers: ";

        assertLog(request, new NoSuchFileException(""), Level.DEBUG, expectedLog);
        assertLog(request, new FileSystemException(""), Level.DEBUG, expectedLog);
    }

    @Test
    public void shouldLogClosedChannelExceptionAtDebugLevel() {
        assertLog(new MockHttpServerRequest("/path"),
            new ClosedChannelException(),
            Level.DEBUG,
            "ClosedChannelException: GET /path");
    }

    private void assertLog(HttpServerRequest<ByteBuf> request, Exception exception, Level expectedLevel, String expectedLog) {
        Appender mockAppender = mock(Appender.class);
        LogManager.getLogger(ExceptionHandler.class).addAppender(mockAppender);
        LogManager.getLogger(ExceptionHandler.class).setLevel(Level.DEBUG);

        HttpServerResponse<ByteBuf> response = new MockHttpServerResponse();
        new ExceptionHandler().handleException(request, response, exception);

        String regex = expectedLog.replaceAll("\\*", ".*")
            .replaceAll("\\[", "\\\\[")
            .replaceAll("\\]", "\\\\]")
            .replaceAll("\\{", "\\\\{")
            .replaceAll("\\}", "\\\\}");

        verify(mockAppender).doAppend(matches(event -> {
            assertThat(event.getLevel()).isEqualTo(expectedLevel);
            assertThat(event.getMessage().toString()).matches(regex);
        }));
    }
}
