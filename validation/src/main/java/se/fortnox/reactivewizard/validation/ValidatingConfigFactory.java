package se.fortnox.reactivewizard.validation;

import se.fortnox.reactivewizard.config.ConfigFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
