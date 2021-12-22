package se.fortnox.reactivewizard.logging;

/**
 * Factory for initializing logging configuration and also container of logging configuration from YAML.
 */
public class LoggingFactory {

    //@Valid
    //@JsonProperty("appenders")
    //Map<String, Map<String, String>> appenders = new HashMap<>();
    //
    //@Valid
    //@JsonProperty("additivity")
    //Map<String, Boolean> additivity;
    //
    //@Valid
    //@JsonProperty("level")
    //String level = "INFO";
    //
    //@Valid
    //@JsonProperty("levels")
    //Map<String, String> levels = new HashMap<>();
    //
    //private static final Map<String, String> TYPE_FROM_NAME = Map.of(
    //    "stdout", "Console",
    //    "file", "RollingFile");
    //
    ///**
    // * Configures logging.
    // */
    public void init() {
        //initLogging();
    }
    //
    //@VisibleForTesting
    //ConfigurationBuilder<BuiltConfiguration> initLogging() {
    //
    //    //Initialize configuration from config.yml
    //    ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
    //    createAppenders(builder);
    //    createLoggers(builder);
    //    final BuiltConfiguration build = builder.build();
    //
    //    //Initialize logging from file
    //    final LoggerContext ctx    = (LoggerContext) LogManager.getContext(false);
    //    final Configuration config = ctx.getConfiguration();
    //
    //    //Set root logger level
    //    config.getRootLogger().setLevel(Level.toLevel(level));
    //
    //    //Overwrite appenders if any
    //    build.getAppenders().forEach((name, appender) -> {
    //        if (config.getAppender(name) != null) {
    //            final RenamedAppender renamedAppender = new RenamedAppender(appender);
    //            config.addAppender(renamedAppender);
    //
    //            if (config.getRootLogger().getAppenders().containsKey(name)) {
    //                config.getRootLogger().removeAppender(name);
    //            }
    //            config.getRootLogger().addAppender(renamedAppender, Level.toLevel(level), null);
    //        }
    //        else {
    //            config.addAppender(appender);
    //        }
    //    });
    //
    //    //Overwrite loggers
    //    build.getLoggers().forEach((name, loggerConfig) -> {
    //
    //        if (config.getLoggers().get(name) != null) {
    //            config.getLoggers().get(name).setLevel(loggerConfig.getLevel());
    //            config.getLoggers().get(name).setAdditive(loggerConfig.isAdditive());
    //        } else {
    //            config.addLogger(name, loggerConfig);
    //        }
    //    });
    //
    //    ctx.updateLoggers();
    //
    //    return builder;
    //}
    //
    //private void createLoggers(ConfigurationBuilder<BuiltConfiguration> builder) {
    //    levels.forEach((loggerName, levelAndAppenderString) -> {
    //        String[] parts = levelAndAppenderString.split(",");
    //        String levelPart = parts[0].trim();
    //
    //        LoggerComponentBuilder logger = builder.newAsyncLogger(loggerName, Level.toLevel(levelPart));
    //        logger.addAttribute("additivity", additivity.getOrDefault(loggerName, false));
    //
    //        if (parts.length > 1) {
    //            String appender = parts[1].trim();
    //            logger.add(builder.newAppenderRef(appender));
    //        }
    //
    //        builder.add(logger);
    //    });
    //}
    //
    //private void createAppenders(ConfigurationBuilder<BuiltConfiguration> builder) {
    //    appenders.forEach((name, appenderProps) -> {
    //        AppenderComponentBuilder appender = builder.newAppender(name, TYPE_FROM_NAME.get(name));
    //        setAppenderAttributes(builder, appenderProps, appender);
    //        builder.add(appender);
    //    });
    //}
    //
    //private void setAppenderAttributes(ConfigurationBuilder<BuiltConfiguration> builder, Map<String, String> appenderProps,
    // AppenderComponentBuilder appender) {
    //    appenderProps.forEach((key, value) -> {
    //        if ("threshold".equals(key)) {
    //            FilterComponentBuilder thresholdFilter = builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY);
    //            thresholdFilter.addAttribute("level", value);
    //            appender.add(thresholdFilter);
    //        } else if ("layout".equals(key)) {
    //            LayoutComponentBuilder layoutBuilder = builder.newLayout(value);
    //            addLayoutProperties(appenderProps, layoutBuilder);
    //            appender.add(layoutBuilder);
    //        } else if ("pattern".equals(key)) {
    //            appender.add(builder.newLayout("PatternLayout").addAttribute("pattern", value));
    //        }  else if (!"layoutProperties".equals(key)) {
    //            appender.addAttribute(key, value);
    //        }
    //    });
    //}
    //
    //void addLayoutProperties(Map<String, String> appenderProps, LayoutComponentBuilder layoutBuilder) {
    //    Splitter.on(" ")
    //        .omitEmptyStrings()
    //        .splitToList(appenderProps.getOrDefault("layoutProperties", ""))
    //        .forEach((layoutProperty) -> {
    //            List<String> propertyKeyValue = Splitter.on("=").splitToList(layoutProperty);
    //            if (propertyKeyValue.size() != 2) {
    //                throw new IllegalArgumentException("Bad formatted layout properties " + layoutProperty);
    //            }
    //            layoutBuilder.addAttribute(propertyKeyValue.get(0), propertyKeyValue.get(1));
    //        });
    //}
    //
    ///**
    // * @return log level of the root logger.
    // */

    public String getLevel() {
        return "info";
    }

    //
    ///**
    // * @param level to set on the root logger.
    // */

    public void setLevel(String level) {
    }
    //
    //private static class RenamedAppender implements Appender {
    //    private final Appender appender;
    //
    //    public RenamedAppender(Appender appender) {
    //        this.appender = appender;
    //    }
    //
    //    @Override
    //    public void append(LogEvent event) {
    //        appender.append(event);
    //    }
    //
    //    @Override
    //    public String getName() {
    //        return appender.getName() + "rw";
    //    }
    //
    //    @Override
    //    public Layout<? extends Serializable> getLayout() {
    //        return appender.getLayout();
    //    }
    //
    //    @Override
    //    public boolean ignoreExceptions() {
    //        return appender.ignoreExceptions();
    //    }
    //
    //    @Override
    //    public ErrorHandler getHandler() {
    //        return appender.getHandler();
    //    }
    //
    //    @Override
    //    public void setHandler(ErrorHandler handler) {
    //        appender.setHandler(handler);
    //    }
    //
    //    @Override
    //    public State getState() {
    //        return appender.getState();
    //    }
    //
    //    @Override
    //    public void initialize() {
    //        appender.initialize();
    //    }
    //
    //    @Override
    //    public void start() {
    //        appender.start();
    //    }
    //
    //    @Override
    //    public void stop() {
    //        appender.stop();
    //    }
    //
    //    @Override
    //    public boolean isStarted() {
    //        return appender.isStarted();
    //    }
    //
    //    @Override
    //    public boolean isStopped() {
    //        return appender.isStopped();
    //    }
    //}
}
