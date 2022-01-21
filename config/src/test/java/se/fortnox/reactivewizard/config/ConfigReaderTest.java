package se.fortnox.reactivewizard.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.junit.Test;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import se.fortnox.reactivewizard.binding.AutoBindModules;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ConfigReaderTest {
    /**
     *
     */
    protected static void setEnv(Map<String, String> newenv) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field    theEnvironmentField     = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> env = (Map<String, String>)theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> cienv = (Map<String, String>)theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class<?>[]          classes = Collections.class.getDeclaredClasses();
                Map<String, String> env     = System.getenv();
                for (Class<?> cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        @SuppressWarnings("unchecked")
                        Map<String, String> map = (Map<String, String>)obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Test
    public void shouldReadConfigFromFile() {
        TestConfig testConfig = ConfigReader.fromFile("src/test/resources/testconfig.yml", TestConfig.class);
        assertThat(testConfig.getMyKey()).isEqualTo("myValue");

        testConfig = ConfigReader.fromTree(ConfigReader.readTree("src/test/resources/testconfig.yml"), TestConfig.class);
        assertThat(testConfig.getMyKey()).isEqualTo("myValue");
    }

    @Test
    public void shouldReadConfigRecordFromFile() {
        TestConfigRecord testConfig = ConfigReader.fromFile("src/test/resources/testconfig.yml", TestConfigRecord.class);
        assertThat(testConfig.myKey()).isEqualTo("myValue");

        testConfig = ConfigReader.fromTree(ConfigReader.readTree("src/test/resources/testconfig.yml"), TestConfigRecord.class);
        assertThat(testConfig.myKey()).isEqualTo("myValue");
    }

    @Test
    public void shouldReplaceEnvPlaceholderWithValue() {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CUSTOM_ENV_VAR", "hello");
        env.put("CUSTOM_ENV_VAR2", "hello again");
        setEnv(env);

        TestConfig testConfig = ConfigReader.fromFile("src/test/resources/testconfig.yml", TestConfig.class);
        assertThat(testConfig.getConfigWithEnvPlaceholder()).isEqualTo("hello");
        assertThat(testConfig.getConfigWithEnvPlaceholder2()).isEqualTo("hello again");
        assertThat(testConfig.getConfigWithEnvPlaceholderInMiddle()).isEqualTo("beforehelloafter");

        testConfig = ConfigReader.fromTree(ConfigReader.readTree("src/test/resources/testconfig.yml"), TestConfig.class);
        assertThat(testConfig.getConfigWithEnvPlaceholder()).isEqualTo("hello");
        assertThat(testConfig.getConfigWithEnvPlaceholder2()).isEqualTo("hello again");
        assertThat(testConfig.getConfigWithEnvPlaceholderInMiddle()).isEqualTo("beforehelloafter");
    }

    @Test
    public void shouldQuoteReplacementString() {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CUSTOM_ENV_VAR", "^THIS.IS.A.\\d{6}T\\d{7}.REGEX1$");
        setEnv(env);

        TestConfig testConfig = ConfigReader.fromFile("src/test/resources/testconfig.yml", TestConfig.class);
        assertThat(testConfig.getConfigWithEnvPlaceholder()).isEqualTo("^THIS.IS.A.\\d{6}T\\d{7}.REGEX1$");

    }

    @Test
    public void shouldReplaceEnvPlaceholderWithEmptyStringIfEnvNotSet() {
        TestConfig testConfig = ConfigReader.fromFile("src/test/resources/testconfig-missing-value.yml", TestConfig.class);
        assertThat(testConfig.getConfigWithEnvPlaceholder()).isNull();
        assertThat(testConfig.getConfigWithEnvPlaceholderInMiddle()).isEqualTo("beforeafter");

        testConfig = ConfigReader.fromTree(ConfigReader.readTree("src/test/resources/testconfig-missing-value.yml"), TestConfig.class);
        assertThat(testConfig.getConfigWithEnvPlaceholder()).isNull();
        assertThat(testConfig.getConfigWithEnvPlaceholderInMiddle()).isEqualTo("beforeafter");
    }

    @Test
    public void shouldReplaceEnvPlaceholderWithEmptyStringIfEnvNotSetForRecord() {
        TestConfigRecord testConfig = ConfigReader.fromFile("src/test/resources/testconfig-missing-value.yml", TestConfigRecord.class);
        assertThat(testConfig.configWithEnvPlaceholder()).isNull();
        assertThat(testConfig.configWithEnvPlaceholderInMiddle()).isEqualTo("beforeafter");

        testConfig = ConfigReader.fromTree(ConfigReader.readTree("src/test/resources/testconfig-missing-value.yml"), TestConfigRecord.class);
        assertThat(testConfig.configWithEnvPlaceholder()).isNull();
        assertThat(testConfig.configWithEnvPlaceholderInMiddle()).isEqualTo("beforeafter");
    }

    @Test
    public void shouldReplaceMultipleEnvPlaceholders() {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("HOST", "localhost");
        env.put("PORT", "8080");
        setEnv(env);
        TestConfig testConfig = ConfigReader.fromFile("src/test/resources/testconfig.yml", TestConfig.class);
        assertThat(testConfig.getUrl()).isEqualTo("http://localhost:8080/test");

        testConfig = ConfigReader.fromTree(ConfigReader.readTree("src/test/resources/testconfig.yml"), TestConfig.class);
        assertThat(testConfig.getUrl()).isEqualTo("http://localhost:8080/test");
    }

    @Test
    public void shouldBindConfigAutomatically() {
        Injector injector = Guice.createInjector(new AutoBindModules(binder->{
            binder.bind(String[].class)
                    .annotatedWith(Names.named("args"))
                    .toInstance(new String[]{"src/test/resources/testconfig.yml"});
        }));
        TestConfig testConfig = injector.getInstance(TestConfig.class);
        assertThat(testConfig.getMyKey()).isEqualTo("myValue");
    }

    @Test
    public void shouldSupportEmptyConfig() {
        EmptyConfig testConfig = ConfigReader.fromFile("src/test/resources/testconfig.yml", EmptyConfig.class);
        assertThat(testConfig).isNotNull();
    }

    @Test
    public void shouldSupportEmptyConfigRecord() {
        EmptyConfigRecord testConfig = ConfigReader.fromFile("src/test/resources/testconfig.yml", EmptyConfigRecord.class);
        assertThat(testConfig).isNotNull();
    }

    @Test
    public void shouldSupportConfigWithAllMissingValues() {
        TestConfig testConfig = ConfigReader.fromFile("src/test/resources/testconfig-empty.yml", TestConfig.class);
        assertThat(testConfig.getMyKey()).isNull();
        assertThat(testConfig.getConfigWithEnvPlaceholder()).isNull();
        assertThat(testConfig.getConfigWithEnvPlaceholderInMiddle()).isNull();
        assertThat(testConfig.getConfigWithEnvPlaceholder2()).isNull();
        assertThat(testConfig.getUrl()).isNull();
    }

    @Test
    public void shouldSupportConfigRecordWithAllMissingValues() {
        TestConfigRecord testConfig = ConfigReader.fromFile("src/test/resources/testconfig-empty.yml", TestConfigRecord.class);
        assertThat(testConfig.myKey()).isNull();
        assertThat(testConfig.configWithEnvPlaceholder()).isNull();
        assertThat(testConfig.configWithEnvPlaceholderInMiddle()).isNull();
        assertThat(testConfig.configWithEnvPlaceholder2()).isNull();
        assertThat(testConfig.url()).isNull();
    }

    @Test
    public void shouldThrowExceptionForInvalidYaml() {
        try {
            ConfigReader.fromFile("src/test/resources/testconfig-invalid.yml", EmptyConfig.class);
            fail("Expected exception, but none was thrown");
        } catch (RuntimeException exception) {
            assertThat(exception).hasRootCauseInstanceOf(MarkedYAMLException.class);
        }
    }

    @Test
    public void shouldReplaceEscapedNewLines() {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CLIENTS", "" +
            "clients:\\n" +
            "    client1:\\n" +
            "      - key41\\n" +
            "      - key24\\n" +
            "    client2:\\n" +
            "      - key55");
        setEnv(env);

        TestConfigNewLine testConfig = ConfigReader.fromFile("src/test/resources/testconfig-newline.yml", TestConfigNewLine.class);
        assertThat(testConfig.getClients().get("client1").get(0)).isEqualTo("key41");
        assertThat(testConfig.getClients().get("client1").get(1)).isEqualTo("key24");
        assertThat(testConfig.getClients().get("client2").get(0)).isEqualTo("key55");
    }
}
