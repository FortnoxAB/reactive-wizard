package se.fortnox.reactivewizard.util;

/**
 * Utilities for debugging in the development environment.
 */
public class DebugUtil {
    public static final boolean IS_DEBUG = isIdePresent();

    /**
     * Checks for the presence of an IDE, currently just IntelliJ.
     */
    private static boolean isIdePresent() {
        ClassLoader classLoader = DebugUtil.class.getClassLoader();
        return classLoader.getResource("com/intellij") != null;
    }

}
