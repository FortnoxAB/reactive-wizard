package se.fortnox.reactivewizard.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import se.fortnox.reactivewizard.config.Config;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
        initLogging();
    }

    @VisibleForTesting
    ConfigurationBuilder<BuiltConfiguration> initLogging() {
        setDefaults();
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        createAppenders(builder);
        createRootLogger(builder);
        createLoggers(builder);
        Configurator.initialize(builder.build());
        return builder;
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

    private void createRootLogger(ConfigurationBuilder<BuiltConfiguration> builder) {
        RootLoggerComponentBuilder rootLogger = builder.newAsyncRootLogger(Level.toLevel(level));
        appenders.keySet().forEach(appenderName -> rootLogger.add(builder.newAppenderRef(appenderName)));
        builder.add(rootLogger);
    }

    private void createAppenders(ConfigurationBuilder<BuiltConfiguration> builder) {
        appenders.forEach((name, appenderProps) -> {
            AppenderComponentBuilder appender = builder.newAppender(name, TYPE_FROM_NAME.get(name));
            setAppenderAttributes(builder, appenderProps, appender);
            builder.add(appender);
        });
    }

    private void setAppenderAttributes(ConfigurationBuilder<BuiltConfiguration> builder, Map<String, String> appenderProps, AppenderComponentBuilder appender) {
        appenderProps.forEach((key, value) -> {
            if ("threshold".equals(key)) {
                FilterComponentBuilder thresholdFilter = builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY);
                thresholdFilter.addAttribute("level", value);
                appender.add(thresholdFilter);
            } else if ("layout".equals(key)) {
                LayoutComponentBuilder layoutBuilder = builder.newLayout(value);
                addLayoutProperties(appenderProps, layoutBuilder);
                appender.add(layoutBuilder);
            } else if ("pattern".equals(key)) {
                appender.add(builder.newLayout("PatternLayout").addAttribute("pattern", value));
            }  else if (!"layoutProperties".equals(key)) {
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

    void addLayoutProperties(Map<String, String> appenderProps, LayoutComponentBuilder layoutBuilder) {
        Splitter.on(" ")
            .omitEmptyStrings()
            .splitToList(appenderProps.getOrDefault("layoutProperties", ""))
            .forEach((layoutProperty) -> {
                List<String> propertyKeyValue = Splitter.on("=").splitToList(layoutProperty);
                if (propertyKeyValue.size() != 2) {
                    throw new IllegalArgumentException("Bad formatted layout properties " + layoutProperty);
                }
                layoutBuilder.addAttribute(propertyKeyValue.get(0), propertyKeyValue.get(1));
            });
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
