package se.fortnox.reactivewizard.logging;

import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LoggingFactoryTest {

    @Test
    public void shouldAddLayoutPropertiesToLayout() throws IOException {
        Map<String, Map<String,String>> appenders      = new HashMap<>();
        appenders.put("stdout", Map.of(
            "layout", "JsonTemplateLayout",
            "layoutProperties", "eventTemplateUri=classpath:LogstashJsonEventLayoutV1.json"
        ));
        LoggingFactory      loggingFactory = new LoggingFactory();
        loggingFactory.appenders = appenders;

        ConfigurationBuilder<BuiltConfiguration> configurationBuilder = loggingFactory.initLogging();

        String config = getConfigAsXml(configurationBuilder);
        assertThat(config)
            .contains("<JsonTemplateLayout eventTemplateUri=\"classpath:LogstashJsonEventLayoutV1.json\"/>");

    }

    @Test
    public void shouldAddLayout() throws IOException {
        Map<String, Map<String,String>> appenders      = new HashMap<>();
        appenders.put("stdout", Map.of(
            "layout", "JsonTemplateLayout"
        ));
        LoggingFactory      loggingFactory = new LoggingFactory();
        loggingFactory.appenders = appenders;

        ConfigurationBuilder<BuiltConfiguration> configurationBuilder = loggingFactory.initLogging();

        String config = getConfigAsXml(configurationBuilder);
        assertThat(config)
            .contains("<JsonTemplateLayout/>");
    }

    @Test
    public void shouldSetupDefaultConfig() throws IOException {
        LoggingFactory      loggingFactory = new LoggingFactory();

        ConfigurationBuilder<BuiltConfiguration> configurationBuilder = loggingFactory.initLogging();

        String config = getConfigAsXml(configurationBuilder);
        assertThat(config)
            .contains("<?xml version=\"1.0\" ?>" +
                "<Configuration><Appenders><Console name=\"stdout\">" +
                "<PatternLayout pattern=\"%-5p [%d{yyyy-MM-dd HH:mm:ss.SSS}] %c: %m%n\"/></Console></Appenders><Loggers><AsyncRoot level=\"INFO\">" +
                "<AppenderRef ref=\"stdout\"/></AsyncRoot></Loggers></Configuration>");
    }

    private String getConfigAsXml(ConfigurationBuilder<BuiltConfiguration> configurationBuilder) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        configurationBuilder.writeXmlConfiguration(byteArrayOutputStream);
        String config = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        return config;
    }

}
