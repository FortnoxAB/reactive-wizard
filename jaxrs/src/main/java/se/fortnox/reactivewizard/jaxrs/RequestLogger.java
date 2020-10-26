package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * Logs incoming requests to a Logger.
 */
public class RequestLogger {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String REDACTED             = "REDACTED";

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
        request.requestHeaders().forEach(header ->
            logLine.append(header.getKey())
                .append('=')
                .append(getHeaderValueOrRedact(header))
                .append(' ')
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
            logLine.append(header.getKey())
                .append('=')
                .append(getHeaderValueOrRedact(header))
                .append(' ')
        );
    }

    /**
     * Returns the value of a header. If the header contains sensitive information the value will be replaced with "REDACTED".
     *
     * @param header The header to get the value from
     * @return value of the header or "REDACTED" if the header contains sensitive information
     */
    public static String getHeaderValueOrRedact(Map.Entry<String, String> header) {
        return getHeaderValueOrRedact(header, emptyList());
    }

    /**
     * Returns the value of a header. If the header contains sensitive information the value will be replaced with "REDACTED".
     *
     * @param header           The header to get the value from
     * @param sensitiveHeaders Sensitive headers to redact
     * @return value of the header or "REDACTED" if the header contains sensitive information
     */
    public static String getHeaderValueOrRedact(Map.Entry<String, String> header, List<String> sensitiveHeaders) {
        if (header == null) {
            return null;
        } else if (AUTHORIZATION_HEADER.equalsIgnoreCase(header.getKey())) {
            return REDACTED;
        } else if (sensitiveHeaders.stream().anyMatch(header.getKey()::equalsIgnoreCase)) {
            return REDACTED;
        }
        return header.getValue();
    }

    /**
     * Redact sensitive information from header values. Any header that contains sensitive information will have the value replaced with "REDACTED"
     *
     * @param headers The headers to map
     * @return headers with sensitive information redacted
     */
    public static Set<Map.Entry<String, String>> getHeaderValuesOrRedact(Map<String, String> headers) {
        return getHeaderValuesOrRedact(headers, emptyList());
    }

    /**
     * Redact sensitive information from header values. Any header that contains sensitive information will have the value replaced with "REDACTED"
     *
     * @param headers          The headers to map
     * @param sensitiveHeaders Sensitive headers that should be redacted from logging
     * @return headers with sensitive information redacted
     */
    public static Set<Map.Entry<String, String>> getHeaderValuesOrRedact(Map<String, String> headers, List<String> sensitiveHeaders) {
        if (headers == null) {
            return Collections.emptySet();
        }
        return headers
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                header -> RequestLogger.getHeaderValueOrRedact(header, sensitiveHeaders)
            ))
            .entrySet();
    }

    /**
     * Append a log entry for a server access event.
     *
     * @param request  The request to log
     * @param response The response to log
     * @param duration Duration of the request
     * @param logLine  StringBuilder to append the header content to
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
     * @param request          The request to log
     * @param response         The response to log
     * @param requestStartTime Duration of the request
     * @param log              The logger to write to
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
