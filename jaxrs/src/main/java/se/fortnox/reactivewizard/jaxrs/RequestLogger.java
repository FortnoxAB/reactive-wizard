package se.fortnox.reactivewizard.jaxrs;

import com.google.common.annotations.VisibleForTesting;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * Assists with logging in the client and server. Allows headers to be marked for masking or redaction.
 */
@Singleton
public class RequestLogger {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String REDACTED = "REDACTED";

    private final Map<String, UnaryOperator<String>> headerTransformationsClient = new HashMap<>();
    private final Map<String, UnaryOperator<String>> headerTransformationsServer = new HashMap<>();

    public RequestLogger() {
        redactAuthorization();
    }

    /**
     * Concatenate request header keys and values into a loggable string.
     *
     * @param request The request to extract headers from
     * @param logLine StringBuilder to append the header content to.
     */
    public void headersToString(HttpServerRequest request, StringBuilder logLine) {
        request.requestHeaders().forEach(header ->
            logLine.append(header.getKey())
                .append('=')
                .append(getHeaderValueOrRedactServer(header))
                .append(' ')
        );
    }

    /**
     * Concatenate response header keys and values into a loggable string.
     *
     * @param response The response to extract headers from
     * @param logLine  StringBuilder to append the header content to
     */
    public void headersToString(HttpServerResponse response, StringBuilder logLine) {
        response.responseHeaders().forEach(header ->
            logLine.append(header.getKey())
                .append('=')
                .append(getHeaderValueOrRedactClient(header))
                .append(' ')
        );
    }

    /**
     * Returns the value of a header for the server. If the header contains sensitive information the value will be replaced with
     * "REDACTED".
     *
     * @param header The header to get the value from
     * @return value of the header or "REDACTED" if the header contains sensitive information
     */
    public String getHeaderValueOrRedactServer(Map.Entry<String, String> header) {
        return getHeaderValueOrRedact(header, headerTransformationsServer);
    }

    /**
     * Returns the value of a header for the client. If the header contains sensitive information the value will be replaced with
     * "REDACTED".
     *
     * @param header The header to get the value from
     * @return value of the header or "REDACTED" if the header contains sensitive information
     */
    public String getHeaderValueOrRedactClient(Map.Entry<String, String> header) {
        return getHeaderValueOrRedact(header, headerTransformationsClient);
    }

    /**
     * Returns the value of a header. If the header contains sensitive information the value will be replaced with "REDACTED".
     *
     * @param header The header to get the value from
     * @return value of the header or the transformed value
     */
    private static String getHeaderValueOrRedact(Map.Entry<String, String> header, Map<String, UnaryOperator<String>> headerTransformers) {
        if (header == null) {
            return null;
        }
        String headerKey = header.getKey().toLowerCase(Locale.ROOT);
        if (headerTransformers.containsKey(headerKey)) {
            return headerTransformers.get(headerKey).apply(header.getValue());
        }
        return header.getValue();
    }

    /**
     * Redact sensitive information from header values in the server. Any header that contains sensitive information will have the value
     * replaced with "REDACTED"
     *
     * @param headers The headers to map
     * @return headers with sensitive information redacted
     */
    public Set<Map.Entry<String, String>> getHeaderValuesOrRedactServer(Map<String, String> headers) {
        if (headers == null) {
            return Set.of();
        }
        return headers.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, this::getHeaderValueOrRedactServer, (a, b) -> a, TreeMap::new))
            .entrySet();
    }

    /**
     * Redact sensitive information from header values in the client. Any header that contains sensitive information will have the value
     * replaced
     * with "REDACTED"
     *
     * @param headers The headers to map
     * @return headers with sensitive information redacted
     */
    public Set<Map.Entry<String, String>> getHeaderValuesOrRedactClient(Map<String, String> headers) {
        if (headers == null) {
            return Set.of();
        }
        return headers.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, this::getHeaderValueOrRedactClient, (a, b) -> a, TreeMap::new))
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
    public void logAccess(HttpServerRequest request, HttpServerResponse response, long duration, StringBuilder logLine) {
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
    public void logRequestResponse(HttpServerRequest request, HttpServerResponse response, long requestStartTime, Logger log) {
        long duration = System.currentTimeMillis() - requestStartTime;
        StringBuilder logLine = new StringBuilder();
        logAccess(request, response, duration, logLine);
        if (log.isDebugEnabled()) {
            logLine.append(" Headers: ");
            headersToString(request, logLine);
            logLine.append(" Response Headers: ");
            headersToString(response, logLine);
            log.debug(logLine.toString());
        } else {
            log.info(logLine.toString());
        }
    }

    public void log(Logger logger, HttpServerRequest request, HttpServerResponse response, long requestStartTime) {
        logRequestResponse(request, response, requestStartTime, logger);
    }

    /**
     * Add header transformation that will be applied to log messages in the client.
     * @param header header key on which to apply transformation (case-insensitive)
     * @param transformation transformation that accepts the original header value and returns the transformed value
     */
    public void addHeaderTransformationClient(String header, UnaryOperator<String> transformation) {
        addHeaderTransformation(header, transformation, headerTransformationsClient);
    }

    /**
     * Mark header as redacted in the client, i.e. in log messages, the header value will be displayed as "REDACTED".
     * @param header header key to be redacted
     */
    public void addRedactedHeaderClient(String header) {
        addHeaderTransformationClient(header, h -> REDACTED);
    }

    /**
     * Add header transformation that will be applied to log messages in the server.
     * @param header header key on which to apply transformation (case-insensitive)
     * @param transformation transformation that accepts the original header value and returns the transformed value
     */
    public void addHeaderTransformationServer(String header, UnaryOperator<String> transformation) {
        addHeaderTransformation(header, transformation, headerTransformationsServer);
    }

    /**
     * Mark header as redacted in the server, i.e. in log messages, the header value will be displayed as "REDACTED".
     * @param header header key to be redacted
     */
    public void addRedactedHeaderServer(String header) {
        addHeaderTransformationServer(header, h -> REDACTED);
    }

    private static void addHeaderTransformation(String header, UnaryOperator<String> transformation, Map<String,
        UnaryOperator<String>> headerTransformations) {
        requireNonNull(header);
        requireNonNull(transformation);
        headerTransformations.put(header.toLowerCase(Locale.ROOT), transformation);
    }

    @VisibleForTesting
    void clearTransformations() {
        headerTransformationsServer.clear();
        headerTransformationsClient.clear();
        redactAuthorization();
    }

    private void redactAuthorization() {
        addRedactedHeaderServer(AUTHORIZATION_HEADER);
        addRedactedHeaderClient(AUTHORIZATION_HEADER);
    }
}
