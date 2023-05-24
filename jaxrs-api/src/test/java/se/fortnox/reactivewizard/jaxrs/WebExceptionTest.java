package se.fortnox.reactivewizard.jaxrs;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

class WebExceptionTest {
    @Test
    void shouldGetIdWhenIntializedWithStatus() {
        WebException webException = new WebException(BAD_GATEWAY);
        assertUuid(webException.getId());
    }

    @Test
    void shouldGetIdWhenInitializedWithErrorCodeAndUserMessage() {
        WebException webException = new WebException(BAD_GATEWAY, "any", "strings");
        assertUuid(webException.getId());
        assertThat(webException.getMessage()).isEqualTo("strings");
    }

    @Test
    void shouldGetIdWhenInitializedWithFieldError() {
        WebException webException = new WebException(new FieldError[0]);
        assertUuid(webException.getId());
    }

    @Test
    void shouldSetErrorLoggingFor500() {
        WebException webException = new WebException(BAD_GATEWAY);
        assertThat(webException.getLogLevel()).isEqualTo(ERROR);
        assertThat(webException.getId()).isNotNull();
    }

    @Test
    void shouldSetDebugLoggingFor404() {
        WebException webException = new WebException(NOT_FOUND);
        assertThat(webException.getLogLevel()).isEqualTo(DEBUG);
    }

    @Test
    void shouldSetWarnLoggingForAnythingElse() {
        WebException webException = new WebException(CONTINUE);
        assertThat(webException.getLogLevel()).isEqualTo(WARN);
    }

    @Test
    void shouldOverrideLogLevelWhenSetUsingWithLogLevel() {
        WebException webException = new WebException(CONTINUE).withLogLevel(ERROR);
        assertThat(webException.getLogLevel()).isEqualTo(ERROR);
    }

    @Test
    void shouldSetErrorCode() {
        WebException webException = new WebException(NOT_FOUND, "entry.not.found");
        assertThat(webException.getError()).isEqualTo("entry.not.found");
    }

    @Test
    void shouldSetErrorCodeToInternalWhenStatusIs500() {
        WebException webException = new WebException(INTERNAL_SERVER_ERROR);
        assertThat(webException.getError()).isEqualTo("internal");
    }

    @Test
    void shouldNotSetErrorCodeToInternalWhenStatusIs500IfErrorCodeWasPassed() {
        WebException webException = new WebException(INTERNAL_SERVER_ERROR, "anything");
        assertThat(webException.getError()).isEqualTo("anything");
    }

    @Test
    void shouldSetBadRequestWhenInitializedWithFieldErrors() {
        WebException webException = new WebException(FieldError.notNull("dummy"));
        assertThat(webException.getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void shouldUsePassedStatusWhenInitializedWithFieldErrorsAndStatus() {
        WebException webException = new WebException(METHOD_NOT_ALLOWED, FieldError.notNull("dummy"));
        assertThat(webException.getStatus()).isEqualTo(METHOD_NOT_ALLOWED);
    }

    @Test
    void shouldSetErrorFromStatusWhenFieldErrorsAreEmpty() {
        WebException webException = new WebException(BAD_REQUEST, new FieldError[0]);
        assertThat(webException.getError()).isEqualTo("badrequest");
    }

    @Test
    void shouldSetErrorFromStatusWhenFieldErrorsAreNull() {
        WebException webException = new WebException(BAD_REQUEST, (FieldError[])null);
        assertThat(webException.getFields()).isNull();
        assertThat(webException.getError()).isEqualTo("badrequest");
    }

    @Test
    void shouldSetErrorToValidationWhenPassedFieldErrors() {
        WebException webException = new WebException(BAD_REQUEST, new FieldError[1]);
        assertThat(webException.getFields()).hasSize(1);
        assertThat(webException.getError()).isEqualTo("validation");
    }

    @Test
    void shouldSetErrorToGivenValueWhenErrorCodeAndThrowableIsGiven() {
        WebException webException = new WebException(BAD_REQUEST, "error_code",new IllegalArgumentException("cause"));
        assertThat(webException.getError()).isEqualTo("error_code");
        assertThat(webException.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSetThrowableToGivenValueWhenErrorCodeAndThrowableIsGiven() {
        WebException webException = new WebException(BAD_REQUEST, "error_code",new IllegalArgumentException("cause"));
        assertThat(webException.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSetErrorParams() {
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
