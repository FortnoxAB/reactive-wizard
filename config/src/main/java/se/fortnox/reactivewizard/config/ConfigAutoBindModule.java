package se.fortnox.reactivewizard.config;

import com.google.inject.Binder;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.ConfigClassScanner;

/**
 * Binds all classes annotated with @{@link Config} to an instance bound to the data read from the config file.
 */
public class ConfigAutoBindModule implements AutoBindModule {

    private final ConfigClassScanner configClassScanner;

    @Inject
    public ConfigAutoBindModule(ConfigClassScanner configClassScanner) {
        this.configClassScanner = configClassScanner;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void configure(Binder binder) {
        Provider<ConfigFactory> configFactoryProvider = binder.getProvider(ConfigFactory.class);
        configClassScanner.getClasses().forEach(cls -> binder.bind(cls).toProvider(configProvider((Class) cls, configFactoryProvider)));
    }

    private <T> Provider<T> configProvider(Class<T> configCls, Provider<ConfigFactory> configFactoryProvider) {
        return () -> configFactoryProvider.get().get(configCls);
    }
}
