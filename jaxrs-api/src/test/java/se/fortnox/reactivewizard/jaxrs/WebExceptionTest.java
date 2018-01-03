package se.fortnox.reactivewizard.jaxrs;

import org.junit.Test;
import org.slf4j.event.Level;

import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.fest.assertions.Assertions.assertThat;
import static org.slf4j.event.Level.*;

public class WebExceptionTest {
    @Test
    public void shouldGetIdWhenIntializedWithStatus() {
        WebException webException = new WebException(BAD_GATEWAY);
        assertUuid(webException.getId());
    }

    @Test
    public void shouldGetIdWhenInitializedWithErrorCodeAndUserMessage() {
        WebException webException = new WebException(BAD_GATEWAY, "any", "strings");
        assertUuid(webException.getId());
        assertThat(webException.getMessage()).isEqualTo("strings");
    }

    @Test
    public void shouldGetIdWhenInitializedWithFieldError() {
        WebException webException = new WebException(new FieldError[0]);
        assertUuid(webException.getId());
    }

    @Test
    public void shouldSetErrorLoggingFor500() {
        WebException webException = new WebException(BAD_GATEWAY);
        assertThat(webException.getLogLevel()).isEqualTo(ERROR);
        assertThat(webException.getId()).isNotNull();
    }

    @Test
    public void shouldSetDebugLoggingFor404() {
        WebException webException = new WebException(NOT_FOUND);
        assertThat(webException.getLogLevel()).isEqualTo(DEBUG);
    }

    @Test
    public void shouldSetWarnLoggingForAnythingElse() {
        WebException webException = new WebException(CONTINUE);
        assertThat(webException.getLogLevel()).isEqualTo(WARN);
    }

    @Test
    public void shouldOverrideLogLevelWhenSetUsingWithLogLevel() {
        WebException webException = new WebException(CONTINUE).withLogLevel(ERROR);
        assertThat(webException.getLogLevel()).isEqualTo(ERROR);
    }

    @Test
    public void shouldSetErrorCode() {
        WebException webException = new WebException(NOT_FOUND, "entry.not.found");
        assertThat(webException.getError()).isEqualTo("entry.not.found");
    }

    @Test
    public void shouldSetErrorCodeToInternalWhenStatusIs500() {
        WebException webException = new WebException(INTERNAL_SERVER_ERROR);
        assertThat(webException.getError()).isEqualTo("internal");
    }

    @Test
    public void shouldNotSetErrorCodeToInternalWhenStatusIs500IfErrorCodeWasPassed() {
        WebException webException = new WebException(INTERNAL_SERVER_ERROR, "anything");
        assertThat(webException.getError()).isEqualTo("anything");
    }

    @Test
    public void shouldSetBadRequestWhenInitializedWithFieldErrors() {
        WebException webException = new WebException(FieldError.notNull("dummy"));
        assertThat(webException.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void shouldUsePassedStatusWhenInitializedWithFieldErrorsAndStatus() {
        WebException webException = new WebException(METHOD_NOT_ALLOWED, FieldError.notNull("dummy"));
        assertThat(webException.getStatus()).isEqualTo(METHOD_NOT_ALLOWED);
    }

    @Test
    public void shouldSetErrorFromStatusWhenFieldErrorsAreEmpty() {
        WebException webException = new WebException(BAD_REQUEST, new FieldError[0]);
        assertThat(webException.getError()).isEqualTo("badrequest");
    }

    @Test
    public void shouldSetErrorFromStatusWhenFieldErrorsAreNull() {
        WebException webException = new WebException(BAD_REQUEST, (FieldError[])null);
        assertThat(webException.getFields()).isNull();
        assertThat(webException.getError()).isEqualTo("badrequest");
    }

    @Test
    public void shouldSetErrorToValidationWhenPassedFieldErrors() {
        WebException webException = new WebException(BAD_REQUEST, new FieldError[1]);
        assertThat(webException.getFields()).hasSize(1);
        assertThat(webException.getError()).isEqualTo("validation");
    }

    @Test
    public void shouldSetErrorParams() {
        WebException webException = new WebException(BAD_REQUEST)
            .withErrorParams("donkey");

        assertThat(webException.getErrorParams()).hasSize(1);
        assertThat(webException.getErrorParams()[0]).isEqualTo("donkey");
    }

    private void assertUuid(String id) {
        Pattern p = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f‌​]{4}-[0-9a-f]{12}$");
        assertThat(p.matcher(id).matches()).isTrue();
    }
}
