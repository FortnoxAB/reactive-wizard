package se.fortnox.reactivewizard.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import se.fortnox.reactivewizard.config.Config;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Factory for initializing logging configuration and also container of logging configuration from YAML.
 */
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

    private static final Map<String, String> TYPE_FROM_NAME = Map.of(
        "stdout", "Console",
        "file", "RollingFile");

    /**
     * Configures logging.
     */
    public void init() {
        setDefaults();
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        RootLoggerComponentBuilder rootLoggerBuilder = createRootLogger(builder);
        createAppendersAndConnectToRootLogger(builder, rootLoggerBuilder);
        createLoggers(builder);
        Configurator.initialize(builder.build());
    }

    private void createLoggers(ConfigurationBuilder<BuiltConfiguration> builder) {
        levels.forEach((loggerName, levelAndAppenderString) -> {
            String[] parts = levelAndAppenderString.split(",");
            String levelPart = parts[0].trim();

            LoggerComponentBuilder logger = builder.newAsyncLogger(loggerName, Level.toLevel(levelPart));
            logger.addAttribute("additivity", additivity.getOrDefault(loggerName, false));

            if (parts.length > 1) {
                String appender = parts[1].trim();
                logger.add(builder.newAppenderRef(appender));
            }

            builder.add(logger);
        });
    }

    private RootLoggerComponentBuilder createRootLogger(ConfigurationBuilder<BuiltConfiguration> builder) {
        RootLoggerComponentBuilder rootLogger = builder.newAsyncRootLogger(Level.toLevel(level));
        builder.add(rootLogger);
        return rootLogger;
    }

    private void createAppendersAndConnectToRootLogger(ConfigurationBuilder<BuiltConfiguration> builder, RootLoggerComponentBuilder rootLoggerBuilder) {
        appenders.forEach((name, appenderProps) -> {
            AppenderComponentBuilder appender = builder.newAppender(
                name,
                requireNonNull(TYPE_FROM_NAME.get(name),
                    "appender names must be either stdout or file, was: " + name)
            );
            setAppenderAttributes(builder, appenderProps, appender);
            builder.add(appender);
            rootLoggerBuilder.add(builder.newAppenderRef(name));
        });
    }

    private void setAppenderAttributes(ConfigurationBuilder<BuiltConfiguration> builder, Map<String, String> appenderProps, AppenderComponentBuilder appender) {
        appenderProps.forEach((key, value) -> {
            if ("threshold".equals(key)) {
                FilterComponentBuilder thresholdFilter = builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY);
                thresholdFilter.addAttribute("level", value);
                appender.add(thresholdFilter);
            } else if ("layout".equals(key)) {
                appender.add(builder.newLayout(value));
            } else if ("pattern".equals(key)) {
                appender.add(builder.newLayout("PatternLayout").addAttribute("pattern", value));
            } else {
                appender.addAttribute(key, value);
            }
        });
    }

    private void setDefaults() {
        if (additivity == null) {
            additivity = new HashMap<>();
        }
        if (appenders == null) {
            appenders = new HashMap<>();
            Map<String, String> stdoutAttributes = new HashMap<>();
            appenders.put("stdout", stdoutAttributes);
            stdoutAttributes.put("pattern", "%-5p [%d{yyyy-MM-dd HH:mm:ss.SSS}] %c: %m%n");
        }
    }

    /**
     * @return log level of the root logger.
     */
    public String getLevel() {
        return level;
    }

    /**
     * @param level to set on the root logger.
     */
    public void setLevel(String level) {
        this.level = level;
    }
}
