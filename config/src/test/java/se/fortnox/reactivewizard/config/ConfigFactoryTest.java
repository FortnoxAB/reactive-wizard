package se.fortnox.reactivewizard.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFactoryTest {

    @Test
    void testConfigFactory() {
        try {
            new ConfigFactory("");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException || e.getCause() instanceof IOException);
        }
    }

    @Test
    void testConfigWithXML() {
        try {
            new ConfigFactory("test.xml");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException || e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    void testConfigWithYML() {
        try {
            new ConfigFactory("src/test/resources/testconfig.yml");
        } catch (Exception e) {
            Assertions.fail("Should handle yml file");
        }
    }

    @Test
    void testConfigWithInjection() {
        try {
            new ConfigFactory(new String[]{"app.jar", "src/test/resources/testconfig.yml"});
        } catch (Exception e) {
            Assertions.fail("Should handle yml file in second argument");
        }
    }

    @Test
    void testConfigWithInjectionOnlyConfig() {
        try {
            new ConfigFactory(new String[]{"src/test/resources/testconfig.yml"});
        } catch (Exception e) {
            Assertions.fail("Should handle yml file in second argument");
        }
    }
}
