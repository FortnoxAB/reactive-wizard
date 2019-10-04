package se.fortnox.reactivewixard.jaxrs;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import se.fortnox.reactivewizard.jaxrs.WebException;

import static java.util.Optional.ofNullable;

/**
 * Matcher for asserting HttpResponseStatus and error code in WebExceptions
 */
public class WebExceptionMatcher extends TypeSafeMatcher<WebException> {

    private HttpResponseStatus foundStatus;
    private String             foundErrorCode;

    private final HttpResponseStatus expectedStatus;
    private final String             expectedErrorCode;

    private WebExceptionMatcher(HttpResponseStatus expectedStatus, String expectedErrorCode) {
        this.expectedStatus    = expectedStatus;
        this.expectedErrorCode = expectedErrorCode;
    }

    @Override
    protected boolean matchesSafely(WebException exception) {
        foundStatus    = exception.getStatus();
        foundErrorCode = exception.getError();

        boolean errorCodeMatches = ofNullable(expectedErrorCode).map(errorCode -> errorCode.equals(foundErrorCode)).orElse(true);
        boolean statusMatches    = ofNullable(expectedStatus).map(status -> status.equals(foundStatus)).orElse(true);

        return errorCodeMatches && statusMatches;
    }

    @Override
    protected void describeMismatchSafely(WebException exception, Description mismatchDescription) {
        if (expectedStatus != null) {
            mismatchDescription.appendText("was " + foundStatus);
        }

        if (expectedErrorCode != null) {
            String and = ofNullable(expectedStatus).map(status -> " and ").orElse("");
            mismatchDescription.appendText(and + foundErrorCode);
        }
    }

    @Override
    public void describeTo(Description description) {
       if (expectedStatus != null) {
           description.appendText("status " + expectedStatus);
       }

       if (expectedErrorCode != null) {
           String and = ofNullable(expectedStatus).map(status -> " and ").orElse(" ");
           description.appendText(and + "error code " + expectedErrorCode);
       }
    }

    public static WebExceptionMatcher has(String errorCode) {
        return new WebExceptionMatcher(null, errorCode);
    }

    public static WebExceptionMatcher has(HttpResponseStatus expectedStatus) {
        return new WebExceptionMatcher(expectedStatus, null);
    }

    public static WebExceptionMatcher has(HttpResponseStatus expectedStatus, String errorCode) {
        return new WebExceptionMatcher(expectedStatus, errorCode);
    }
}
