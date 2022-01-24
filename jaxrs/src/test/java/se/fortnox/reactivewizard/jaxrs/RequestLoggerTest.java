package se.fortnox.reactivewizard.jaxrs;

import org.junit.After;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RequestLoggerTest {

    private final RequestLogger requestLogger = new RequestLogger();

    @After
    public void cleanup() {
        requestLogger.clearTransformations();
    }

    @Test
    public void shouldRedactAuthorizationValue() {
        var header = entry("Authorization", "secret");

        assertEquals("REDACTED", requestLogger.getHeaderValueOrRedactIncoming(header));
        assertEquals("REDACTED", requestLogger.getHeaderValueOrRedactOutgoing(header));
    }

    @Test
    public void shouldNotRedactNoneAuthorizationValue() {
        var header = entry("OtherHeader", "notasecret");

        assertEquals("notasecret", requestLogger.getHeaderValueOrRedactIncoming(header));
        assertEquals("notasecret", requestLogger.getHeaderValueOrRedactOutgoing(header));
    }

    @Test
    public void shouldRedactAuthorizationValues() {
        var headers = Map.of(
            "Authorization", "secret",
            "OtherHeader", "notasecret"
        );
        var expectedValue = Map.of(
            "Authorization", "REDACTED",
            "OtherHeader", "notasecret"
        );

        assertThat(requestLogger.getHeaderValuesOrRedactIncoming(headers))
            .isEqualTo(expectedValue.entrySet());
        assertThat(requestLogger.getHeaderValuesOrRedactOutgoing(headers))
            .isEqualTo(expectedValue.entrySet());
    }

    @Test
    public void shouldRedactSuppliedSensitiveHeader() {
        var headers = Map.of(
            "OtherHeader", "notasecret",
            "Cookie", "oreo"
        );
        requestLogger.addRedactedHeaderIncoming("Cookie");
        requestLogger.addRedactedHeaderOutgoing("Cookie");
        var expectedValue = Map.of(
            "Cookie", "REDACTED",
            "OtherHeader", "notasecret"
        ).entrySet();

        assertThat(requestLogger.getHeaderValuesOrRedactIncoming(headers))
            .containsExactlyInAnyOrderElementsOf(expectedValue);
        assertThat(requestLogger.getHeaderValuesOrRedactOutgoing(headers))
            .containsExactlyInAnyOrderElementsOf(expectedValue);
    }

    @Test
    public void shouldReturnEmpty_getHeaderValuesOrRedact() {
        assertThat(requestLogger.getHeaderValuesOrRedactIncoming(null))
            .isEmpty();
        assertThat(requestLogger.getHeaderValuesOrRedactOutgoing(null))
            .isEmpty();
    }

    @Test
    public void shouldReturnNull_getHeaderValueOrRedact() {
        assertNull(requestLogger.getHeaderValueOrRedactIncoming(null));
        assertNull(requestLogger.getHeaderValueOrRedactOutgoing(null));
    }

    @Test
    public void shouldTransformHeaders() {
        var headers = Map.of(
            "header1", "value1",
            "header2", "value2",
            "header3", "vaLue3",
            "header4", "value4",
            "header5", "value5"
        );

        requestLogger.addHeaderTransformationIncoming("header2", header -> header.replaceAll("\\d", "*"));
        requestLogger.addHeaderTransformationIncoming("header3", String::toUpperCase);

        Set<Map.Entry<String, String>> resultIncoming = requestLogger.getHeaderValuesOrRedactIncoming(headers);

        assertThat(resultIncoming)
            .hasSize(5)
            .contains(
                entry("header1", "value1"),
                entry("header2", "value*"),
                entry("header3", "VALUE3"),
                entry("header4", "value4"),
                entry("header5", "value5")
            );

        requestLogger.addHeaderTransformationOutgoing("header1", header -> header.replaceAll("\\d", "X"));
        requestLogger.addHeaderTransformationOutgoing("header3", String::toLowerCase);

        Set<Map.Entry<String, String>> resultOutgoing = requestLogger.getHeaderValuesOrRedactOutgoing(headers);

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
