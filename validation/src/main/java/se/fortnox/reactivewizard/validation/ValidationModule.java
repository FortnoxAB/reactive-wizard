package se.fortnox.reactivewizard.validation;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.InjectAnnotatedScanner;
import se.fortnox.reactivewizard.config.ConfigFactory;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;

import java.util.Optional;

public class ValidationModule implements AutoBindModule {

    private final InjectAnnotatedScanner injectAnnotatedScanner;

    @Inject
    public ValidationModule(InjectAnnotatedScanner injectAnnotatedScanner) {
        this.injectAnnotatedScanner = injectAnnotatedScanner;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Validator.class).toProvider(() -> {
            return Validation
                    .byDefaultProvider()
                    .configure()
                    .parameterNameProvider(new JaxRsParameterNameResolver())
                    .buildValidatorFactory()
                    .getValidator();
        }).in(Scopes.SINGLETON);


        // Validate config
        binder.bind(ConfigFactory.class).to(ValidatingConfigFactory.class).in(Scopes.SINGLETON);

        // Validate all resources
        Provider<ValidatorUtil> validatorUtilProvider = binder.getProvider(ValidatorUtil.class);
        injectAnnotatedScanner.getClasses().forEach(cls -> {
            Optional<Class<?>> jaxRsClass = JaxRsMeta.getJaxRsClass(cls);
            if (jaxRsClass.isPresent() && jaxRsClass.get().isInterface()) {
                bindValidationProxy(binder, validatorUtilProvider, (Class)cls, jaxRsClass.get());
            }
        });
    }

    private <T> void bindValidationProxy(Binder binder, Provider<ValidatorUtil> validatorUtilProvider, Class<T> originalClass, Class<T> interfaceClass) {
        Provider<T> originalProvider = binder.getProvider(originalClass);
        Provider<T> validatingProvider = validatingProvider(interfaceClass, originalProvider, validatorUtilProvider);
        binder.bind(interfaceClass).toProvider(validatingProvider).in(Scopes.SINGLETON);
    }

    private <T> Provider<T> validatingProvider(Class<T> iface, Provider<T> wrappedProvider, Provider<ValidatorUtil> validatorUtilProvider) {
        return () -> ValidatingProxy.create(iface, wrappedProvider.get(), validatorUtilProvider.get());
    }
}
