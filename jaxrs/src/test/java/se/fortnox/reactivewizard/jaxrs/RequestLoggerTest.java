package se.fortnox.reactivewizard.jaxrs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestLoggerTest {

    private final RequestLogger requestLogger = new RequestLogger();

    @AfterEach
    public void cleanup() {
        requestLogger.clearTransformations();
    }

    @Test
    void shouldRedactAuthorizationValue() {
        var header = entry("Authorization", "secret");

        assertEquals("REDACTED", requestLogger.getHeaderValueOrRedactServer(header));
        assertEquals("REDACTED", requestLogger.getHeaderValueOrRedactClient(header));
    }

    @Test
    void shouldNotRedactNoneAuthorizationValue() {
        var header = entry("OtherHeader", "notasecret");

        assertEquals("notasecret", requestLogger.getHeaderValueOrRedactServer(header));
        assertEquals("notasecret", requestLogger.getHeaderValueOrRedactClient(header));
    }

    @Test
    void shouldRedactAuthorizationValues() {
        var headers = Map.of(
            "Authorization", "secret",
            "OtherHeader", "notasecret"
        );
        var expectedValue = Map.of(
            "Authorization", "REDACTED",
            "OtherHeader", "notasecret"
        );

        assertThat(requestLogger.getHeaderValuesOrRedactServer(headers))
            .isEqualTo(expectedValue.entrySet());
        assertThat(requestLogger.getHeaderValuesOrRedactClient(headers))
            .isEqualTo(expectedValue.entrySet());
    }

    @Test
    void shouldRedactSuppliedSensitiveHeader() {
        var headers = Map.of(
            "OtherHeader", "notasecret",
            "Cookie", "oreo"
        );
        requestLogger.addRedactedHeaderServer("Cookie");
        requestLogger.addRedactedHeaderClient("Cookie");
        var expectedValue = Map.of(
            "Cookie", "REDACTED",
            "OtherHeader", "notasecret"
        ).entrySet();

        assertThat(requestLogger.getHeaderValuesOrRedactServer(headers))
            .containsExactlyInAnyOrderElementsOf(expectedValue);
        assertThat(requestLogger.getHeaderValuesOrRedactClient(headers))
            .containsExactlyInAnyOrderElementsOf(expectedValue);
    }

    @Test
    void shouldReturnEmpty_getHeaderValuesOrRedact() {
        assertThat(requestLogger.getHeaderValuesOrRedactServer(null))
            .isEmpty();
        assertThat(requestLogger.getHeaderValuesOrRedactClient(null))
            .isEmpty();
    }

    @Test
    void shouldReturnNull_getHeaderValueOrRedact() {
        assertNull(requestLogger.getHeaderValueOrRedactServer(null));
        assertNull(requestLogger.getHeaderValueOrRedactClient(null));
    }

    @Test
    void shouldTransformHeaders() {
        var headers = Map.of(
            "header1", "value1",
            "header2", "value2",
            "header3", "vaLue3",
            "header4", "value4",
            "header5", "value5"
        );

        requestLogger.addHeaderTransformationServer("header2", header -> header.replaceAll("\\d", "*"));
        requestLogger.addHeaderTransformationServer("header3", String::toUpperCase);

        Set<Map.Entry<String, String>> resultIncoming = requestLogger.getHeaderValuesOrRedactServer(headers);

        assertThat(resultIncoming)
            .hasSize(5)
            .contains(
                entry("header1", "value1"),
                entry("header2", "value*"),
                entry("header3", "VALUE3"),
                entry("header4", "value4"),
                entry("header5", "value5")
            );

        requestLogger.addHeaderTransformationClient("header1", header -> header.replaceAll("\\d", "X"));
        requestLogger.addHeaderTransformationClient("header3", String::toLowerCase);

        Set<Map.Entry<String, String>> resultOutgoing = requestLogger.getHeaderValuesOrRedactClient(headers);

        assertThat(resultOutgoing)
            .hasSize(5)
            .contains(
                entry("header1", "valueX"),
                entry("header2", "value2"),
                entry("header3", "value3"),
                entry("header4", "value4"),
                entry("header5", "value5")
            );
    }
}
