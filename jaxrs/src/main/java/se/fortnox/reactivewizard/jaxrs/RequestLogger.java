package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * Logs incoming requests to a Logger.
 */
public class RequestLogger {
    private final Logger logger;

    public RequestLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Concatenate request header keys and values into a loggable string.
     *
     * @param request The request to extract headers from
     * @param logLine StringBuilder to append the header content to.
     */
    public static void headersToString(HttpServerRequest request, StringBuilder logLine) {
        request.requestHeaders().forEach(e ->
            logLine.append(e.getKey()).append('=').append(e.getValue()).append(' ')
        );
    }

    /**
     * Concatenate response header keys and values into a loggable string.
     *
     * @param response The response to extract headers from
     * @param logLine  StringBuilder to append the header content to
     */
    public static void headersToString(HttpServerResponse response, StringBuilder logLine) {
        response.responseHeaders().forEach(header ->
            logLine.append(header.getKey()).append('=').append(header.getValue()).append(' ')
        );
    }

    /**
     * Append a log entry for a server access event.
     *
     * @param request The request to log
     * @param response The response to log
     * @param duration Duration of the request
     * @param logLine StringBuilder to append the header content to
     */
    public static void logAccess(HttpServerRequest request, HttpServerResponse response, long duration, StringBuilder logLine) {
        HttpResponseStatus status = response.status();
        logLine.append(status == null ? "0" : status.code())
            .append(": ")
            .append(request.method())
            .append(" ")
            .append(request.uri())
            .append(" ")
            .append(duration);
    }

    /**
     * Write a log entry for a request/response pair.
     *
     * @param request The request to log
     * @param response The response to log
     * @param requestStartTime Duration of the request
     * @param log The logger to write to
     */
    public static void logRequestResponse(HttpServerRequest request, HttpServerResponse response, long requestStartTime, Logger log) {
        long          duration = System.currentTimeMillis() - requestStartTime;
        StringBuilder logLine  = new StringBuilder();
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

    public void log(HttpServerRequest request, HttpServerResponse response, long requestStartTime) {
        logRequestResponse(request, response, requestStartTime, logger);
    }
}
