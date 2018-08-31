package se.fortnox.reactivewizard.util;

import org.junit.Test;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class DebugUtilTest {

    @Test
    public void testIsDebug() {
        assertThat(DebugUtil.IS_DEBUG).isNotNull(); // will be true if running from Intellij IDEA and false from command line
    }

    @Test
    public void testIsIdePresent() throws MalformedURLException {
        ClassLoader classLoaderWithIdeaDebugger = Mockito.mock(ClassLoader.class);
        when(classLoaderWithIdeaDebugger.getResource(eq("com/intellij")))
                .thenReturn(new URL("jar:file:/path/to/debugger-agent-storage.jar!/com/intellij"));

        assertThat(DebugUtil.isIdePresent(classLoaderWithIdeaDebugger)).isTrue();
    }

    @Test
    public void testIsMavenDebug() {
        String debugPropName = "maven.surefire.debug";
        boolean originalSurefireDebugProp = Boolean.getBoolean(debugPropName);
        System.setProperty(debugPropName, "true");

        assertThat(DebugUtil.isMavenDebug()).isTrue();

        // Clean up
        System.setProperty(debugPropName, String.valueOf(originalSurefireDebugProp));
    }
}
