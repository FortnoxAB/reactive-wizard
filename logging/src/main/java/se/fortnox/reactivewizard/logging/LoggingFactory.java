package se.fortnox.reactivewizard.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.blitz4j.LoggingConfiguration;
import io.reactiverse.reactivecontexts.core.Context;
import se.fortnox.reactivewizard.config.Config;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Factory for initializing logging configuration and also container of logging configuration from YAML.
 */
@Config("logging")
public class LoggingFactory {

    static {
        // Initializing reactive contexts propagation
        Context.load();
    }

    @Valid
    @JsonProperty("appenders")
    Map<String, Map<String, String>> appenders;

    @Valid
    @JsonProperty("additivity")
    Map<String, Boolean> additivity;

    @Valid
    @JsonProperty("level")
    String level = "INFO";

    @Valid
    @JsonProperty("levels")
    Map<String, String> levels = new HashMap<>();

    public void init() {
        if (appenders == null) {
            appenders = new HashMap<>();
            appenders.put("stdout", new HashMap<>());
        }

        Properties props = new Properties();
        loadConfigurationsFromFiles(props);
        props.setProperty("log4j.rootLogger", level + "," + getAppenderNames());

        addAppenderSettings(props);

        addAdditivity(props);

        for (Entry<String, String> e : levels.entrySet()) {
            props.setProperty("log4j.logger." + e.getKey(), e.getValue());
        }
        LoggingConfiguration.getInstance().configure(props);
    }

    private void loadConfigurationsFromFiles(Properties props) {
        try {
            Enumeration<URL> resources = LoggingFactory.class.getClassLoader().getResources("log4j.properties");
            while (resources.hasMoreElements()) {
                URL log4jFile = resources.nextElement();
                System.setProperty("log4j.configuration", log4jFile.toString());
                try (InputStream stream = log4jFile.openStream()) {
                    props.load(stream);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addAdditivity(Properties props) {
        if (additivity == null) {
            return;
        }
        additivity.forEach((key, value) -> props.put("log4j.additivity." + key, String.valueOf(value)));
    }

    private void addAppenderSettings(Properties props) {
        for (Entry<String, Map<String, String>> appenderConfig : appenders.entrySet()) {
            for (Entry<String, String> setting : appenderConfig.getValue().entrySet()) {
                StringBuilder key = new StringBuilder();
                key.append("log4j.appender.").append(appenderConfig.getKey());
                if (!"class".equals(setting.getKey())) {
                    key.append(".").append(setting.getKey());
                }

                props.put(key.toString(), setting.getValue());
            }
        }
    }

    private String getAppenderNames() {
        return String.join(",", appenders.keySet());
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
