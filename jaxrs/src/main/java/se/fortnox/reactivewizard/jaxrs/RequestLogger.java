package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import org.slf4j.Logger;

/**
 * Logs incoming requests to a Logger
 */
public class RequestLogger {
    private final Logger logger;

    public RequestLogger(Logger logger) {
        this.logger = logger;
    }

    public void log(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response, long requestStartTime) {
        logRequestResponse(request, response, requestStartTime, logger);
    }

    public static void headersToString(HttpServerRequest<ByteBuf> request, StringBuilder logLine) {
        request.headerIterator().forEachRemaining(e->
                logLine.append(e.getKey()).append('=').append(e.getValue()).append(' ')
        );
    }

    public static void headersToString(HttpServerResponse<ByteBuf> response, StringBuilder logLine) {
        response.getHeaderNames().forEach(key->
                logLine.append(key).append('=').append(response.getHeader(key)).append(' ')
        );
    }

    public static void logAccess(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response, long duration, StringBuilder logLine) {
        logLine.append(response.getStatus().code())
                .append(": ")
                .append(request.getHttpMethod())
                .append(" ")
                .append(request.getUri())
                .append(" ")
                .append(duration);
    }

    public static void logRequestResponse(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response, long requestStartTime, Logger log) {
        long duration = System.currentTimeMillis() - requestStartTime;
        StringBuilder logLine = new StringBuilder();
        RequestLogger.logAccess(request, response, duration, logLine);
        if (log.isDebugEnabled()) {
            logLine.append(" Headers: ");
            RequestLogger.headersToString(request, logLine);
            logLine.append(" Response Headers: ");
            RequestLogger.headersToString(response, logLine);
            log.debug(logLine.toString());
        } else {
            log.info(logLine.toString());
        }
    }
}
