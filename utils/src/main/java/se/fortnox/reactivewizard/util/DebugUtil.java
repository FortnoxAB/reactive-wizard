package se.fortnox.reactivewizard.util;

/**
 * Utilities for debugging in the development environment.
 */
public class DebugUtil {
    public static final boolean IS_DEBUG = isIdePresent(DebugUtil.class.getClassLoader()) || isMavenDebug();

    /**
     * Checks for the presence of an IDE, currently just IntelliJ.
     * @param classLoader to use for the check
     */
    static boolean isIdePresent(ClassLoader classLoader) {
        return false;
    }

    static boolean isMavenDebug() {
        return Boolean.getBoolean("maven.surefire.debug");
    }
}
