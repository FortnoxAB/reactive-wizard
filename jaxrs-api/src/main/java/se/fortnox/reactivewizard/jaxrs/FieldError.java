package se.fortnox.reactivewizard.jaxrs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Map;

public class FieldError {

    protected static final String VALIDATION = "validation.";
    private final String              field;
    private final String              error;
    private final Map<String, Object> errorParams;

    public FieldError(String field, String validationErrorCode) {
        this(field, validationErrorCode, null);
    }

    public FieldError(String field, String validationErrorCode, Map<String, Object> errorParams) {
        this.field = field;
        this.errorParams = errorParams;
        this.error = addPrefix(validationErrorCode);
    }

    public static FieldError notNull(String field) {
        return new FieldError(field, "notnull");
    }

    private String addPrefix(String error) {
        if (!error.startsWith(VALIDATION)) {
            return VALIDATION + error.toLowerCase();
        }
        return error.toLowerCase();
    }

    public String getField() {
        return field;
    }

    public String getError() {
        return error;
    }

    @JsonInclude(Include.NON_NULL)
    public Map<String, Object> getErrorParams() {
        return errorParams;
    }

}
