package se.fortnox.reactivewizard;

import io.netty.handler.codec.http.HttpMethod;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.Exceptions;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.mocks.MockHttpServerRequest;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;
import se.fortnox.reactivewizard.test.LoggingMockUtil;
import se.fortnox.reactivewizard.test.LoggingVerifier;
import se.fortnox.reactivewizard.test.LoggingVerifierExtension;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LoggingVerifierExtension.class)
class ExceptionHandlerTest {

    LoggingVerifier loggingVerifier = new LoggingVerifier(ExceptionHandler.class);

    @Test
    void shouldLogHeadersForErrors() {
        MockHttpServerRequest request = new MockHttpServerRequest("/path");
        request.requestHeaders().add("X-DBID", "5678");
        String expectedLog = """
            500 Internal Server Error
            \tCause: runtime exception
            \tResponse: {"id":"*","error":"internal"}
            \tRequest: GET /path headers: X-DBID=5678\s""";
        assertLog(request, new RuntimeException("runtime exception"), ERROR, expectedLog);
    }

    @Test
    void shouldLogHeadersForWarnings() {
        MockHttpServerRequest request = new MockHttpServerRequest("/path");
        request.requestHeaders().add("X-DBID", "5678");
        String expectedLog = """
            400 Bad Request
            \tCause: -
            \tResponse: {"id":"*","error":"badrequest"}
            \tRequest: GET /path headers: X-DBID=5678\s""";
        assertLog(request,
            new WebException(BAD_REQUEST),
            WARN,
            expectedLog);
    }

    @Test
    void shouldRedactSensitiveHeaders() {
        MockHttpServerRequest request = new MockHttpServerRequest("/path");
        request.requestHeaders()
            .add("Authorization", "secret")
            .add("OtherHeader", "notasecret");
        String expectedLog = """
            400 Bad Request
            \tCause: -
            \tResponse: {"id":"*","error":"badrequest"}
            \tRequest: GET /path headers: Authorization=REDACTED OtherHeader=notasecret\s""";
        assertLog(request,
            new WebException(BAD_REQUEST),
            WARN,
            expectedLog);
    }

    @Test
    void shouldRedactSensitiveHeadersSpecified() {

        MockHttpServerRequest request = new MockHttpServerRequest("/path");
        request.requestHeaders()
            .add("Authorization", "secret")
            .add("OtherHeader", "notasecret");
        String expectedLog = """
            400 Bad Request
            \tCause: -
            \tResponse: {"id":"*","error":"badrequest"}
            \tRequest: GET /path headers: Authorization=REDACTED OtherHeader=REDACTED\s""";
        assertLog(request,
            new WebException(BAD_REQUEST),
            WARN,
            expectedLog, new ExceptionHandler() {
                @Override
                protected String getHeaderValue(Map.Entry<String, String> header) {
                    if (header != null && header.getKey().equalsIgnoreCase("OtherHeader")) {
                        return "REDACTED";
                    }
                    return super.getHeaderValue(header);
                }
            }, null);
    }

    @Test
    void shouldLog404AsDebug() {
        Level originalLevel = LoggingMockUtil.setLevel(ExceptionHandler.class, DEBUG);
        try {
            MockHttpServerRequest request = new MockHttpServerRequest("/path");
            String expectedLog = """
                404 Not Found
                \tCause: -
                \tResponse: {"id":"*","error":"notfound"}
                \tRequest: GET /path headers:\s""";

            assertLog(request, new NoSuchFileException(""), DEBUG, expectedLog);
            assertLog(request, new FileSystemException(""), DEBUG, expectedLog);
        } finally {
            LoggingMockUtil.setLevel(ExceptionHandler.class, originalLevel);
        }
    }

    @Test
    void shouldLogClosedChannelExceptionAtDebugLevel() {
        Level originalLevel = LoggingMockUtil.setLevel(ExceptionHandler.class, DEBUG);
        ClosedChannelException closedChannelException = new ClosedChannelException();
        try {
            assertLog(new MockHttpServerRequest("/path"),
                closedChannelException,
                DEBUG,
                "Inbound connection has been closed: GET /path",
                closedChannelException);
        } finally {
            LoggingMockUtil.setLevel(ExceptionHandler.class, originalLevel);
        }
    }

    @Test
    void shouldLogCancellationExceptionAtDebugLevel() {
        Level originalLevel = LoggingMockUtil.setLevel(ExceptionHandler.class, DEBUG);
        CancellationException cancellationException = new CancellationException();
        try {
            assertLog(new MockHttpServerRequest("/path"),
                cancellationException,
                DEBUG,
                "Inbound connection has been closed: GET /path",
                cancellationException
            );
        } finally {
            LoggingMockUtil.setLevel(ExceptionHandler.class, originalLevel);
        }
    }

    @Test
    void shouldLogNativeIoExceptionAtDebugLevel() {
        Level originalLevel = LoggingMockUtil.setLevel(ExceptionHandler.class, DEBUG);
        IOException brokenPipeException = new IOException("writevAddresses(..) failed: Broken pipe");
        try {
            assertLog(new MockHttpServerRequest("/path"),
                brokenPipeException,
                DEBUG,
                "Inbound connection has been closed: GET /path",
                brokenPipeException
            );
        } finally {
            LoggingMockUtil.setLevel(ExceptionHandler.class, originalLevel);
        }
    }

    @Test
    void shouldReturnLastExceptionOfCompositeException() {
        WebException firstException = new WebException(INTERNAL_SERVER_ERROR).withLogLevel(org.slf4j.event.Level.DEBUG);
        WebException secondException = new WebException(BAD_GATEWAY).withLogLevel(org.slf4j.event.Level.WARN);
        Exception compositeException = Exceptions.multiple(firstException, secondException);

        String expectedLog = """
            502 Bad Gateway
            \tCause: -
            \tResponse: {"id":"*","error":"badgateway"}
            \tRequest: GET /path headers:\s""";

        assertLog(new MockHttpServerRequest("/path"), compositeException, WARN, expectedLog);
    }

    @Test
    void shouldReturnCauseOfOnErrorThrowable() {
        WebException cause = new WebException(BAD_GATEWAY).withLogLevel(org.slf4j.event.Level.WARN);

        Exception onErrorThrowable = (Exception) Exceptions.wrapSource(cause);

        String expectedLog = """
            502 Bad Gateway
            \tCause: -
            \tResponse: {"id":"*","error":"badgateway"}
            \tRequest: GET /path headers:\s""";

        assertLog(new MockHttpServerRequest("/path"), onErrorThrowable, WARN, expectedLog);
    }

    @Test
    void shouldSetLogLevelFromWebException() {
        Level originalLevel = LoggingMockUtil.setLevel(ExceptionHandler.class, DEBUG);
        try {
            WebException cause = new WebException(BAD_GATEWAY);

            cause.setLogLevel(org.slf4j.event.Level.WARN);
            assertLog(new MockHttpServerRequest("/path"), cause, WARN, "*");

            cause.setLogLevel(org.slf4j.event.Level.DEBUG);
            assertLog(new MockHttpServerRequest("/path"), cause, DEBUG, "*");

            cause.setLogLevel(org.slf4j.event.Level.ERROR);
            assertLog(new MockHttpServerRequest("/path"), cause, ERROR, "*");

            cause.setLogLevel(org.slf4j.event.Level.TRACE);
            assertLog(new MockHttpServerRequest("/path"), cause, DEBUG, "*");

            cause.setLogLevel(org.slf4j.event.Level.INFO);
            assertLog(new MockHttpServerRequest("/path"), cause, Level.INFO, "*");
        } finally {
            LoggingMockUtil.setLevel(ExceptionHandler.class, originalLevel);
        }
    }

    @Test
    void shouldSetContentLengthZeroFromHEAD() {
        MockHttpServerRequest request = new MockHttpServerRequest("/path", HttpMethod.HEAD);
        HttpServerResponse response = new MockHttpServerResponse();
        WebException exception = new WebException(BAD_GATEWAY);
        new ExceptionHandler().handleException(request, response, exception);

        assertThat(response.responseHeaders().get("Content-Length")).isEqualTo("0");
    }

    private void assertLog(HttpServerRequest request, Exception exception, Level expectedLevel, String expectedLog) {
        this.assertLog(request, exception, expectedLevel, expectedLog, new ExceptionHandler(), null);
    }

    private void assertLog(HttpServerRequest request, Exception exception, Level expectedLevel, String expectedLog, Exception expectedLoggedException) {
        this.assertLog(request, exception, expectedLevel, expectedLog, new ExceptionHandler(), expectedLoggedException);
    }

    private void assertLog(HttpServerRequest request, Exception exception, Level expectedLevel, String expectedLog, ExceptionHandler exceptionHandler, Exception expectedLoggedException) {
        HttpServerResponse response = new MockHttpServerResponse();
        exceptionHandler.handleException(request, response, exception);

        String regex = expectedLog.replaceAll("\\*", ".*")
            .replaceAll("\\[", "\\\\[")
            .replaceAll("]", "\\\\]")
            .replaceAll("\\{", "\\\\{")
            .replaceAll("}", "\\\\}");

        loggingVerifier.assertThatLogs()
            .anySatisfy(event -> {
                assertThat(event.getLevel())
                    .isEqualTo(expectedLevel);
                if (expectedLoggedException != null) {
                    assertThat(event.getThrown())
                        .isEqualTo(expectedLoggedException);
                }
                if (!expectedLog.equals("*")) {
                    assertThat(event.getMessage().getFormattedMessage()).matches(regex);
                }
            });
    }
}
