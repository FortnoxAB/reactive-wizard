package se.fortnox.reactivewizard.config;

import com.fasterxml.jackson.databind.JsonNode;

import javax.inject.Inject;
import javax.inject.Named;

public class ConfigFactory {
    private JsonNode tree;

    @Inject
    public ConfigFactory(@Named("args") String[] args) {
        this(args[args.length - 1]);
    }

    public ConfigFactory(String configFile) {
        if (configFile != null && configFile.endsWith(".yml")) {
            tree = ConfigReader.readTree(configFile);
        } else {
            throw new IllegalArgumentException("Only yml configuration implemented, you tried with: " + configFile);
        }
    }

    public <T> T get(Class<T> cls) {
        return ConfigReader.fromTree(tree, cls);
    }

}
