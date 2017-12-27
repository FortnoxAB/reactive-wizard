package se.fortnox.reactivewizard.config;

import com.fasterxml.jackson.databind.JsonNode;

public class ConfigFactory {
    private JsonNode tree;

    public ConfigFactory(String configFile) {
        tree = ConfigReader.readTree(configFile);
    }

    public <T> T get(Class<T> cls) {
        return ConfigReader.fromTree(tree, cls);
    }

}
