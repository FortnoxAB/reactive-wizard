package se.fortnox.reactivewizard.validation;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import se.fortnox.reactivewizard.config.ConfigFactory;

@Singleton
public class ValidatingConfigFactory extends ConfigFactory {
    private final ValidatorUtil validatorUtil;

    @Inject
    public ValidatingConfigFactory(@Named("args") String[] args, ValidatorUtil validatorUtil) {
        super(args);
        this.validatorUtil = validatorUtil;
    }

    @Override
    public <T> T get(Class<T> cls) {
        T config = super.get(cls);
        validatorUtil.validate(config);
        return config;
    }
}
