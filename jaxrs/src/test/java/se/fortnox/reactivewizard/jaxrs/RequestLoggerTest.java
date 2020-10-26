package se.fortnox.reactivewizard.jaxrs;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RequestLoggerTest {

    @Test
    public void shouldRedactAuthorizationValue() {
        Map.Entry<String,String> header = new AbstractMap.SimpleEntry<>("Authorization", "secret");

        String result = RequestLogger.getHeaderValueOrRedact(header);
        assertEquals("REDACTED", result);
    }

    @Test
    public void shouldNotRedactAuthorizationValue() {
        Map.Entry<String,String> header = new AbstractMap.SimpleEntry<>("OtherHeader", "notasecret");
        String result = RequestLogger.getHeaderValueOrRedact(header);
        assertEquals("notasecret", result);
    }

    @Test
    public void shouldRedactAuthorizationValues() {
        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization", "secret");
        headers.put("OtherHeader", "notasecret");

        Set<Map.Entry<String, String>> result = RequestLogger.getHeaderValuesOrRedact(headers);

        Map<String, String> expectedValue = new HashMap<>();
        expectedValue.put("Authorization", "REDACTED");
        expectedValue.put("OtherHeader", "notasecret");
        assertTrue(CollectionUtils.isEqualCollection(result, expectedValue.entrySet()));
    }

    @Test
    public void shouldRedactSuppliedSensitiveHeader() {
        Map<String,String> headers = new HashMap<>();
        headers.put("OtherHeader", "notasecret");
        headers.put("Cookie", "oreo");

        Set<Map.Entry<String, String>> result = RequestLogger.getHeaderValuesOrRedact(headers, singletonList("cookie"));

        Map<String, String> expectedValue = new HashMap<>();
        expectedValue.put("Cookie", "REDACTED");
        expectedValue.put("OtherHeader", "notasecret");
        assertTrue(CollectionUtils.isEqualCollection(result, expectedValue.entrySet()));
    }

    @Test
    public void shouldReturnNull_getHeaderValuesOrRedact() {
        Set<Map.Entry<String, String>> result = RequestLogger.getHeaderValuesOrRedact(null);
        assertTrue(CollectionUtils.isEqualCollection(result, Collections.emptySet()));
    }

    @Test
    public void shouldReturnNull_getHeaderValueOrRedact() {
        assertNull(RequestLogger.getHeaderValueOrRedact(null));
    }
}
