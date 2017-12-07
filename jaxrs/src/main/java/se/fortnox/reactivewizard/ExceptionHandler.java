package se.fortnox.reactivewizard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.exceptions.CompositeException;
import rx.exceptions.OnErrorThrowable;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.json.InvalidJsonException;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.nio.channels.ClosedChannelException;
import java.nio.file.FileSystemException;
import java.util.List;

import static rx.Observable.empty;
import static rx.Observable.just;

/**
 * Handles exceptions and writes errors to the response and the log.
 */
public class ExceptionHandler {

    private static Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);
    private ObjectMapper mapper;

    @Inject
    public ExceptionHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ExceptionHandler() {
        this(new ObjectMapper());
    }

    public Observable<Void> handleException(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response, Throwable throwable) {
        if (throwable instanceof OnErrorThrowable) {
            throwable = throwable.getCause();
        }

        if (throwable instanceof CompositeException) {
            CompositeException compositeException = (CompositeException)throwable;
            List<Throwable>    exceptions         = compositeException.getExceptions();
            throwable = exceptions.get(exceptions.size() - 1);
        }

        WebException webException;
        if (throwable instanceof FileSystemException) {
            webException = new WebException(HttpResponseStatus.NOT_FOUND);
        } else if (throwable instanceof InvalidJsonException) {
            webException = new WebException(HttpResponseStatus.BAD_REQUEST, "invalidjson", throwable.getMessage());
        } else if (throwable instanceof WebException) {
            webException = (WebException)throwable;
        } else if (throwable instanceof ClosedChannelException) {
            LOG.debug("ClosedChannelException: " + request.getHttpMethod() + " " + request.getUri(), throwable);
            return Observable.empty();
        } else {
            webException = new WebException(HttpResponseStatus.INTERNAL_SERVER_ERROR, throwable);
        }

        if (webException.getStatus().code() >= 500) {
            LOG.error(getLogMessage(request, webException), webException);
        } else {
            if (webException.getStatus() != HttpResponseStatus.NOT_FOUND) {
                // No log for 404
                LOG.warn(getLogMessage(request, webException), webException);
            }
        }

        response = response.setStatus(webException.getStatus());
        if (HttpMethod.HEAD.equals(request.getHttpMethod())) {
            response.addHeader("Content-Length", 0);
        } else {
            response = response.addHeader("Content-Type", MediaType.APPLICATION_JSON);
            return response.writeString(just(json(webException)));
        }
        return empty();
    }

    private String json(WebException webException) {
        try {
            return mapper.writeValueAsString(webException);
        } catch (JsonProcessingException e) {
            LOG.error("Error writing json for exception " + webException, e);
            return null;
        }
    }

    private String getLogMessage(HttpServerRequest<ByteBuf> request,
        WebException webException
    ) {
        final StringBuilder msg = new StringBuilder()
            .append(webException.getStatus().toString())
            .append("\n\tCause: ").append(webException.getCause() != null ?
                webException.getCause().getMessage() :
                "-")
            .append("\n\tResponse: ").append(json(webException))
            .append("\n\tRequest: ")
            .append(request.getHttpMethod())
            .append(" ").append(request.getUri())
            .append(" headers: ");
        request.headerIterator().forEachRemaining(h ->
            msg
                .append(h.getKey())
                .append('=')
                .append(h.getValue())
                .append(' ')
        );
        return msg.toString();
    }

}
