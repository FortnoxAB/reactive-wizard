package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import se.fortnox.reactivewixard.jaxrs.WebExceptionMatcher;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

public class WebExceptionMatcherTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldExpectCorrectErrorCode() {
        String expectedErrorCode = "error.code";

        expectedException.expect(WebException.class);
        expectedException.expect(WebExceptionMatcher.has(expectedErrorCode));

        throwWebException(null, expectedErrorCode);
    }

    @Test
    public void shouldExpectCorrectStatus() {
        HttpResponseStatus expectedStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;

        expectedException.expect(WebException.class);
        expectedException.expect(WebExceptionMatcher.has(expectedStatus));

        throwWebException(expectedStatus, null);
    }

    @Test
    public void shouldExpectCorrectStatusAndErrorCode() {
        HttpResponseStatus expectedStatus    = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        String             expectedErrorCode = "error.code";

        expectedException.expect(WebException.class);
        expectedException.expect(WebExceptionMatcher.has(expectedStatus, expectedErrorCode));

        throwWebException(expectedStatus, expectedErrorCode);
    }

    @Test
    public void shouldNotExpectErrorAsDefault() {
        int sum = 1 + 1;

        assertThat(sum).isEqualTo(2);
    }

    private static void throwWebException(HttpResponseStatus expectedStatus, String expectedErrorCode) {
        HttpResponseStatus status = ofNullable(expectedStatus).orElse(HttpResponseStatus.BAD_REQUEST);

        throw new WebException(status, expectedErrorCode);
    }
}
