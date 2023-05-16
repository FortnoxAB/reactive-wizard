package se.fortnox.reactivewizard.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.POJONode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class ConfigFactory {
    private JsonNode tree;

    @Inject
    public ConfigFactory(@Named("args") String[] args) {
        this(args.length == 0 ? null : args[args.length - 1]);
    }

    public ConfigFactory(String configFile) {
        if (configFile == null) {
            tree = new POJONode(new Object());
            return;
        }
        if (!configFile.endsWith(".yml")) {
            throw new IllegalArgumentException("Only yml configuration implemented, you tried with: " + configFile);
        }
        tree = ConfigReader.readTree(configFile);
    }

    public <T> T get(Class<T> cls) {
        return ConfigReader.fromTree(tree, cls);
    }
}
