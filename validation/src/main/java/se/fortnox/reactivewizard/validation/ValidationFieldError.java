package se.fortnox.reactivewizard.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path.Node;

import se.fortnox.reactivewizard.jaxrs.FieldError;


class ValidationFieldError extends FieldError {

	public ValidationFieldError(ConstraintViolation<Object> v) {
		super(getPath(v), getErrorFromConstraintValidation(v), extractErrorParams(v));
	}

	private static String getPath(ConstraintViolation<Object> v) {
		if (v.getExecutableParameters() != null) {
			Iterator<Node> pathIterator = v.getPropertyPath().iterator();
			String pathName = null;
			while (pathIterator.hasNext()) {
				pathName = pathIterator.next().getName();
			}
			return pathName;
		}
		return v.getPropertyPath().toString();
	}

	private static String getErrorFromConstraintValidation(ConstraintViolation<Object> v) {
		String error = v.getMessageTemplate();

		//Extract error code from message templates like "{javax.validation.constraints.Size.message}"
		error = error.replaceAll("\\{.*\\.constraints\\.(.*)\\.message\\}", "$1");

		return error;
	}

	private static Map<String, Object> extractErrorParams(ConstraintViolation<Object> v) {
		Map<String, Object> attributes = v.getConstraintDescriptor().getAttributes();

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
		int i = 0;
		for (ConstraintViolation<Object> v : constraintViolations) {
			fieldErrors[i++] = new ValidationFieldError(v);
		}
		return fieldErrors;
	}

	private static boolean keyIsErrorParam(String key) {
		return !key.equals("groups") && !key.equals("message") && !key.equals("payload");
	}

}
