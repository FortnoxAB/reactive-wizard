package se.fortnox.reactivewizard.logging;

public class LoggingFactoryTest {

    //@Test
    //public void shouldAddLayoutPropertiesToLayout() throws IOException {
    //    Map<String, Map<String,String>> appenders      = new HashMap<>();
    //    appenders.put("stdout", Map.of(
    //        "layout", "JsonTemplateLayout",
    //        "layoutProperties", "eventTemplateUri=classpath:LogstashJsonEventLayoutV1.json"
    //    ));
    //    LoggingFactory      loggingFactory = new LoggingFactory();
    //    loggingFactory.appenders = appenders;
    //
    //    ConfigurationBuilder<BuiltConfiguration> configurationBuilder = loggingFactory.initLogging();
    //
    //    String config = getConfigAsXml(configurationBuilder);
    //    assertThat(config)
    //        .contains("<JsonTemplateLayout eventTemplateUri=\"classpath:LogstashJsonEventLayoutV1.json\"/>");
    //}
    //
    //@Test
    //public void shouldReadSystemPropertyForAdditionalConfiguration() throws IOException {
    //    Map<String, Map<String,String>> appenders      = new HashMap<>();
    //    appenders.put("stdout", Map.of(
    //        "layout", "JsonTemplateLayout"
    //    ));
    //    LoggingFactory      loggingFactory = new LoggingFactory();
    //    loggingFactory.appenders = appenders;
    //
    //    System.setProperty("logging-config", "log4j2.yml");
    //    ConfigurationBuilder<BuiltConfiguration> configurationBuilder = loggingFactory.initLogging();
    //
    //    String config = getConfigAsXml(configurationBuilder);
    //    assertThat(config)
    //        .contains("<JsonTemplateLayout/>");
    //}
    //
    //@Test
    //public void shouldAddLayout() throws IOException {
    //    Map<String, Map<String,String>> appenders      = new HashMap<>();
    //    appenders.put("stdout", Map.of(
    //        "layout", "JsonTemplateLayout"
    //    ));
    //    LoggingFactory      loggingFactory = new LoggingFactory();
    //    loggingFactory.appenders = appenders;
    //
    //    ConfigurationBuilder<BuiltConfiguration> configurationBuilder = loggingFactory.initLogging();
    //
    //    String config = getConfigAsXml(configurationBuilder);
    //    assertThat(config)
    //        .contains("<JsonTemplateLayout/>");
    //
    //    final LoggerContext ctx    = (LoggerContext) LogManager.getContext(false);
    //    final Configuration configuration = ctx.getConfiguration();
    //
    //    assertThat(configuration.getRootLogger().getAppenders().containsKey("stdoutrw"));
    //}
    //
    //@Test
    //public void shouldSetupDefaultConfig() {
    //
    //    final LoggerContext ctx    = (LoggerContext) LogManager.getContext(false);
    //    final Configuration config = ctx.getConfiguration();
    //
    //    assertThat(config.getRootLogger().getAppenders().containsKey("stdout"));
    //}
    //
    //private String getConfigAsXml(ConfigurationBuilder<BuiltConfiguration> configurationBuilder) throws IOException {
    //    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    //    configurationBuilder.writeXmlConfiguration(byteArrayOutputStream);
    //    String config = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    //    return config;
    //}

}
