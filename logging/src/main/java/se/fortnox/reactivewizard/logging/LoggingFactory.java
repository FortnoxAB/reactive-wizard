package se.fortnox.reactivewizard.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.blitz4j.LoggingConfiguration;
import se.fortnox.reactivewizard.config.Config;

import javax.validation.Valid;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

@Config("logging")
public class LoggingFactory {

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
            appenders = new HashMap<String, Map<String, String>>();
            appenders.put("stdout", new HashMap<String, String>());
        }

        URL log4jfile = LoggingFactory.class.getResource("/log4j.properties");
        System.setProperty("log4j.configuration", log4jfile.toString());

        Properties props = new Properties();
        props.setProperty("log4j.rootLogger", level + "," + getAppenderNames());

        addAppenderSettings(props);

        addAdditivity(props);

        for (Entry<String, String> e : levels.entrySet()) {
            props.setProperty("log4j.logger." + e.getKey(), e.getValue());
        }
        LoggingConfiguration.getInstance().configure(props);
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
                props.put("log4j.appender." + appenderConfig.getKey() + "." + setting.getKey(), setting.getValue());
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
