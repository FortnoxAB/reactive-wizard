package se.fortnox.reactivewizard.validation;

import se.fortnox.reactivewizard.jaxrs.FieldError;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.annotation.Annotation;
import java.util.List;

import static java.util.stream.Collectors.toList;

public abstract class CustomEntityValidator<A extends Annotation, T> implements ConstraintValidator<A, T> {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Override
	public boolean isValid(T value, ConstraintValidatorContext constraintValidatorContext) {
		List<FieldError> fieldErrors = isValid(value);
		if (fieldErrors == null || fieldErrors.isEmpty()) {
			return true;
		}

		constraintValidatorContext.disableDefaultConstraintViolation();
		for (FieldError error : fieldErrors) {
			constraintValidatorContext
				.buildConstraintViolationWithTemplate(error.getError())
				.addPropertyNode(error.getField())
				.addConstraintViolation();
		}

		return false;
	}

	protected List<FieldError> validate(Object entity) {
		return validator.validate(entity)
			.stream()
			.map(ValidationFieldError::new)
			.collect(toList());
	}

	public abstract List<FieldError> isValid(T value);
}