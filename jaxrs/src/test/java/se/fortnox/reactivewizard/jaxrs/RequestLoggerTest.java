package se.fortnox.reactivewizard.jaxrs;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RequestLoggerTest {

    @Test
    public void shouldRedactAuthorizationValue() {
        Map.Entry<String,String> header = new AbstractMap.SimpleEntry<>("Authorization", "secret");

        String result = RequestLogger.getHeaderValueOrRedact(header);
        assertEquals(result, "REDACTED");
    }

    @Test
    public void shouldNotRedactAuthorizationValue() {
        Map.Entry<String,String> header = new AbstractMap.SimpleEntry<>("OtherHeader", "notasecret");
        String result = RequestLogger.getHeaderValueOrRedact(header);
        assertEquals(result, "notasecret");
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
}
