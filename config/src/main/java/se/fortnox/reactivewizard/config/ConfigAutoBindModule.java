package se.fortnox.reactivewizard.config;

import com.google.inject.Binder;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.ConfigClassScanner;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Binds all classes annotated with @{@link Config} to an instance bound to the data read from the config file
 */
public class ConfigAutoBindModule implements AutoBindModule {

    private final String configFileName;
    private final ConfigClassScanner configClassScanner;

    @Inject
    public ConfigAutoBindModule(ConfigClassScanner configClassScanner, @Named("args") String [] args) {
        this.configClassScanner = configClassScanner;
        this.configFileName = args[args.length - 1];
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void configure(Binder binder) {
        ConfigFactory configFactory = new ConfigFactory(configFileName);
        binder.bind(ConfigFactory.class).toInstance(configFactory);
        configClassScanner.getClasses().forEach(cls -> binder.bind(cls).toProvider(new ConfigProvider(cls, configFactory)));
    }
}