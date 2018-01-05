package se.fortnox.reactivewizard.config;

import javax.inject.Provider;

public class ConfigProvider<T> implements Provider<T> {

    private Class<T> cls;
    private ConfigFactory configFactory;

    public ConfigProvider(Class<T> cls, ConfigFactory configFactory) {
        this.cls = cls;
        this.configFactory = configFactory;
    }

    @Override
    public T get() {
        return configFactory.get(cls);
    }

}
