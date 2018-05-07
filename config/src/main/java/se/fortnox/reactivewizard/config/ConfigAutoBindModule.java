package se.fortnox.reactivewizard.config;

import com.google.inject.Binder;
import com.google.inject.Provider;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.ConfigClassScanner;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Binds all classes annotated with @{@link Config} to an instance bound to the data read from the config file.
 */
public class ConfigAutoBindModule implements AutoBindModule {

    private final ConfigFactory      configFactory;
    private final ConfigClassScanner configClassScanner;

    @Inject
    public ConfigAutoBindModule(ConfigFactory configFactory, ConfigClassScanner configClassScanner) {
        this.configFactory = configFactory;
        this.configClassScanner = configClassScanner;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void configure(Binder binder) {
        configClassScanner.getClasses().forEach(cls -> binder.bind(cls).toProvider(new ConfigProvider(cls, configFactory)));
    }
}
