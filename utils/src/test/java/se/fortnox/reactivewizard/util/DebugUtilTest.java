package se.fortnox.reactivewizard.util;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class DebugUtilTest {

    @Test
    public void testIsDebug() {
        String debugPropName = "maven.surefire.debug";
        boolean originalSurefireDebugProp = Boolean.getBoolean(debugPropName);
        System.setProperty(debugPropName, "true");

        assertThat(DebugUtil.IS_DEBUG).isTrue();

        System.setProperty(debugPropName, String.valueOf(originalSurefireDebugProp));
    }
}
