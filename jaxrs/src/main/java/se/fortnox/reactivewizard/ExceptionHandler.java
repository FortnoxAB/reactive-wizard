package se.fortnox.reactivewizard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpMethod;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.channel.AbortedException;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.json.InvalidJsonException;

import java.nio.channels.ClosedChannelException;
import java.nio.file.FileSystemException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static reactor.core.Exceptions.isMultiple;
import static reactor.core.Exceptions.unwrap;
import static reactor.core.Exceptions.unwrapMultiple;
import static reactor.core.publisher.Flux.empty;
import static reactor.core.publisher.Mono.justOrEmpty;

/**
 * Handles exceptions and writes errors to the response and the log.
 */
public class ExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);
    private static final String INBOUND_CONNECTION_CLOSED = "Inbound connection has been closed: {} {}";
    private final ObjectMapper mapper;
    private final RequestLogger requestLogger;

    @Inject
    public ExceptionHandler(ObjectMapper mapper, RequestLogger requestLogger) {
        this.mapper = mapper;
        this.requestLogger = requestLogger;
    }

    public ExceptionHandler() {
        this(new ObjectMapper(), new RequestLogger());
    }

    /**
     * Handle exceptions from server requests.
     *
     * @param request   The current request
     * @param response  The current response
     * @param throwable The exception that occurred
     * @return success or error
     */
    public Publisher<Void> handleException(HttpServerRequest request, HttpServerResponse response, Throwable throwable) {
        throwable = unwrap(throwable);

        if (isMultiple(throwable)) {
            List<Throwable> exceptions = unwrapMultiple(throwable);
            throwable = exceptions.get(exceptions.size() - 1);
        }

        WebException webException;
        if (throwable instanceof FileSystemException) {
            webException = new WebException(NOT_FOUND);
        } else if (throwable instanceof InvalidJsonException) {
            webException = new WebException(BAD_REQUEST, "invalidjson", throwable.getMessage());
        } else if (throwable instanceof WebException we) {
            webException = we;
        } else if (throwable instanceof ClosedChannelException || throwable instanceof AbortedException || throwable instanceof CancellationException) {
            LOG.atDebug()
                .setMessage(INBOUND_CONNECTION_CLOSED)
                .addArgument(request::method)
                .addArgument(request::uri)
                .setCause(throwable)
                .log();
            return empty();
        } else {
            webException = new WebException(INTERNAL_SERVER_ERROR, throwable);
        }

        logException(request, webException);

        response = response.status(webException.getStatus());
        if (HttpMethod.HEAD.equals(request.method())) {
            response.addHeader("Content-Length", "0");
        } else {
            response = response.addHeader("Content-Type", APPLICATION_JSON);
            return response.sendString(justOrEmpty(json(webException)));
        }
        return empty();
    }

    private String json(WebException webException) {
        try {
            return mapper.writeValueAsString(webException);
        } catch (JsonProcessingException e) {
            LOG.error("Error writing json for exception {}", webException, e);
            return null;
        }
    }

    private String getLogMessage(HttpServerRequest request, WebException webException) {
        final StringBuilder msg = new StringBuilder()
            .append(webException.getStatus().toString())
            .append("\n\tCause: ").append(webException.getCause() != null ?
                webException.getCause().getMessage() :
                "-")
            .append("\n\tResponse: ").append(json(webException))
            .append("\n\tRequest: ")
            .append(request.method())
            .append(" ").append(request.uri())
            .append(" headers: ");

        request.requestHeaders().forEach(header ->
            msg
                .append(header.getKey())
                .append('=')
                .append(getHeaderValue(header))
                .append(' ')
        );
        return msg.toString();
    }

    /**
     * Override this to specify own logic to handle certain headers.
     * <p>
     * Typically redact sensitive stuff.
     *
     * @param header the header entry where the Map.Entry key is the header name and the Map.Entry value is the header value
     * @return the string that should be logged for this particular header in case of an Exception.
     */
    protected String getHeaderValue(Map.Entry<String, String> header) {
        return requestLogger.getHeaderValueOrRedactServer(header);
    }

    private void logException(HttpServerRequest request, WebException webException) {
        String logMessage = getLogMessage(request, webException);
        switch (webException.getLogLevel()) {
            case WARN -> LOG.warn(logMessage, webException);
            case INFO -> LOG.info(logMessage, webException);
            case DEBUG, TRACE -> LOG.debug(logMessage, webException);
            default -> LOG.error(logMessage, webException);
        }
    }
}
