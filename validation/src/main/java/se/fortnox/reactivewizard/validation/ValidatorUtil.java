package se.fortnox.reactivewizard.validation;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;

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
        throwIfError(validator.validate(obj));
    }

    public void validateParameters(Object object, Method method, Object[] parameterValues) {
        throwIfError(validator.forExecutables().validateParameters(object, method, parameterValues));
        for (Object obj : parameterValues) {
            if (obj != null) {
                Class<?> cls = obj.getClass();
                if (!cls.isPrimitive() && !cls.getName().startsWith("java.")) {
                    validate(obj);
                }
            }
        }
    }

    private static void throwIfError(Set<ConstraintViolation<Object>> result) {
        if (!result.isEmpty()) {
            throw new ValidationFailedException(result);
        }
    }
}
