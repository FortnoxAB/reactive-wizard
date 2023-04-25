package se.fortnox.reactivewizard.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.Set;

@Singleton
public class ValidatorUtil {

    private final Validator validator;

    @Inject
    public ValidatorUtil(Validator validator) {
        this.validator = validator;
    }

    public ValidatorUtil() {
        this(Validation
                .byDefaultProvider()
                .configure()
                .parameterNameProvider(new JaxRsParameterNameResolver())
                .buildValidatorFactory()
                .getValidator());
    }

    public void validate(Object obj) {
        if (obj == null) {
            return;
        }
        Class<?> cls = obj.getClass();
        if (cls.isPrimitive() || cls.getName().startsWith("java.")) {
            return;
        }
        throwIfError(validator.validate(obj));
    }

    /**
     * Validate contraints for parameters of the given method.
     * @param object the object of method
     * @param method the method to validate
     * @param parameterValues the provided values
     */
    public void validateParameters(Object object, Method method, Object[] parameterValues) {
        throwIfError(validator.forExecutables().validateParameters(object, method, parameterValues));
        for (Object obj : parameterValues) {
            if (obj instanceof Iterable<?>) {
                ((Iterable<?>) obj).forEach(this::validate);
            } else {
                validate(obj);
            }
        }
    }

    private static void throwIfError(Set<ConstraintViolation<Object>> result) {
        if (!result.isEmpty()) {
            throw new ValidationFailedException(result);
        }
    }
}
