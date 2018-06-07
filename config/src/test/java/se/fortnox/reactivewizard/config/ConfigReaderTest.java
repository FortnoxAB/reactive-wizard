package se.fortnox.reactivewizard.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.junit.Test;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.test.TestUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static se.fortnox.reactivewizard.test.TestUtil.assertException;

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
    public void shouldReplaceEnvPlaceholderWithValue() {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CUSTOM_ENV_VAR", "hello");
        setEnv(env);

        TestConfig testConfig = ConfigReader.fromFile("src/test/resources/testconfig.yml", TestConfig.class);
        assertThat(testConfig.getConfigWithEnvPlaceholder()).isEqualTo("hello");
        assertThat(testConfig.getConfigWithEnvPlaceholderInMiddle()).isEqualTo("beforehelloafter");

        testConfig = ConfigReader.fromTree(ConfigReader.readTree("src/test/resources/testconfig.yml"), TestConfig.class);
        assertThat(testConfig.getConfigWithEnvPlaceholder()).isEqualTo("hello");
        assertThat(testConfig.getConfigWithEnvPlaceholderInMiddle()).isEqualTo("beforehelloafter");
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
    public void shouldThrowExceptionForInvalidYaml() {
        try {
            ConfigReader.fromFile("src/test/resources/testconfig-invalid.yml", EmptyConfig.class);
            fail("Expected exception, but none was thrown");
        } catch (RuntimeException exception) {
            assertException(exception, MarkedYAMLException.class);
        }
    }
}
