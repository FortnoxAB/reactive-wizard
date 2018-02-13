package se.fortnox.reactivewizard.util;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class DebugUtilTest {

    @Test
    public void testIsIdePresent() {
        assertThat(DebugUtil.IS_DEBUG).isNotNull();
    }
}
