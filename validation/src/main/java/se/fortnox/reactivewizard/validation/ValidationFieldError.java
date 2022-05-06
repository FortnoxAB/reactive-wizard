package se.fortnox.reactivewizard.validation;

import se.fortnox.reactivewizard.jaxrs.FieldError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path.Node;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


class ValidationFieldError extends FieldError {

    public ValidationFieldError(ConstraintViolation<Object> violation) {
        super(getPath(violation), getErrorFromConstraintValidation(violation), extractErrorParams(violation));
    }

    private static String getPath(ConstraintViolation<Object> violation) {
        if (violation.getExecutableParameters() != null) {
            Iterator<Node> pathIterator = violation.getPropertyPath().iterator();
            String pathName = null;
            while (pathIterator.hasNext()) {
                pathName = pathIterator.next().getName();
            }
            return pathName;
        }
        return violation.getPropertyPath().toString();
    }

    private static String getErrorFromConstraintValidation(ConstraintViolation<Object> violation) {
        String error = violation.getMessageTemplate();

        //Extract error code from message templates like "{javax.validation.constraints.Size.message}"
        error = error.replaceAll("\\{.*\\.constraints\\.(.*)\\.message\\}", "$1");

        return error;
    }

    private static Map<String, Object> extractErrorParams(ConstraintViolation<Object> violation) {
        Map<String, Object> attributes = violation.getConstraintDescriptor().getAttributes();

        Map<String, Object> errorParams = null;

        for (Entry<String, Object> attribute : attributes.entrySet()) {
            String key = attribute.getKey();

            if (!keyIsErrorParam(key)) {
                continue;
            }

            if (errorParams == null) {
                errorParams = new HashMap<>();
            }
            errorParams.put(key, attribute.getValue());
        }
        return errorParams;
    }

    public static FieldError[] from(Set<ConstraintViolation<Object>> constraintViolations) {
        if (constraintViolations == null) {
            return null;
        }
        FieldError[] fieldErrors = new FieldError[constraintViolations.size()];
        int index = 0;
        for (ConstraintViolation<Object> v : constraintViolations) {
            fieldErrors[index++] = new ValidationFieldError(v);
        }
        return fieldErrors;
    }

    private static boolean keyIsErrorParam(String key) {
        return !key.equals("groups") && !key.equals("message") && !key.equals("payload");
    }

}
