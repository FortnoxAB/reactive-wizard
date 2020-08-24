package se.fortnox.reactivewizard.jaxrs;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

import java.util.AbstractMap;
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
        Map<String,String> headers = Map.of(
            "Authorization", "secret",
            "OtherHeader", "notasecret"
        );
        Set<Map.Entry<String, String>> result = RequestLogger.getHeaderValuesOrRedact(headers);
        Set<Map.Entry<String, String>> expectedValue = Map.of(
            "Authorization", "REDACTED",
            "OtherHeader", "notasecret"
        ).entrySet();
        assertTrue(CollectionUtils.isEqualCollection(result, expectedValue));
    }
}
