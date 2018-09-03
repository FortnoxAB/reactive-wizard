package se.fortnox.reactivewizard.validation;

import java.lang.annotation.Annotation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public abstract class CustomFieldValidator<A extends Annotation, T> implements ConstraintValidator<A, T> {

    @Override
    public boolean isValid(T value, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(value);
    }

    public abstract boolean isValid(T value);
}