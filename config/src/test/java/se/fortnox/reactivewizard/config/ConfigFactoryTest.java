package se.fortnox.reactivewizard.config;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class ConfigFactoryTest {

    @Test
    public void testConfigFactory() {
        try {
            new ConfigFactory("");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException || e.getCause() instanceof IOException);
        }
    }

    @Test
    public void testConfigWithXML() {
        try {
            new ConfigFactory("test.xml");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException || e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testConfigWithYML() {
        try {
            new ConfigFactory("src/test/resources/testconfig.yml");
        } catch (Exception e) {
            Assert.fail("Should handle yml file");
        }
    }

    @Test
    public void testConfigWithInjection() {
        try {
            new ConfigFactory(new String[]{"app.jar", "src/test/resources/testconfig.yml"});
        } catch (Exception e) {
            Assert.fail("Should handle yml file in second argument");
        }
    }

    @Test
    public void testConfigWithInjectionOnlyConfig() {
        try {
            new ConfigFactory(new String[]{"src/test/resources/testconfig.yml"});
        } catch (Exception e) {
            Assert.fail("Should handle yml file in second argument");
        }
    }
}
