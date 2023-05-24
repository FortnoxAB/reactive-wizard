package se.fortnox.reactivewizard.jaxrs;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class FieldErrorTest {

    @Test
    void shouldPrefixErrorWithValidationWhenMissing() {
        FieldError fieldError = new FieldError("username", "too.short");
        assertThat(fieldError.getError()).isEqualTo("validation.too.short");
        assertThat(fieldError.getField()).isEqualTo("username");
    }

    @Test
    void shouldNotPrefixErrorWithValidationWhenAlreadyPresent() {
        FieldError fieldError = new FieldError("username", "validation.too.short");
        assertThat(fieldError.getError()).isEqualTo("validation.too.short");
        assertThat(fieldError.getField()).isEqualTo("username");
    }

    @Test
    void shouldInitializeNotNullError() {
        FieldError fieldError = FieldError.notNull("username");
        assertThat(fieldError.getError()).isEqualTo("validation.notnull");
        assertThat(fieldError.getField()).isEqualTo("username");
    }

    @Test
    void shouldIncludeErrorParams() {
        Map<String, Object> errorParams = new HashMap<>();
        errorParams.put("name", "a");

        FieldError fieldError = new FieldError("username", "validation.too.short", errorParams);
        assertThat(fieldError.getError()).isEqualTo("validation.too.short");
        assertThat(fieldError.getField()).isEqualTo("username");
        assertThat(fieldError.getErrorParams()).contains(entry("name", "a"));
    }

    @Test
    void shouldNotInitializeValues() {
        FieldError fieldError = new FieldError();
        assertThat(fieldError.getError()).isNull();
        assertThat(fieldError.getField()).isNull();
        assertThat(fieldError.getErrorParams()).isNull();
    }
}
