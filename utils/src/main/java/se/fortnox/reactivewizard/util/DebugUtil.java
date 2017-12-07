package se.fortnox.reactivewizard.util;

public class DebugUtil {
    public static final boolean IS_DEBUG = isIdePresent();

    private static boolean isIdePresent() {
        ClassLoader classLoader = DebugUtil.class.getClassLoader();
        return classLoader.getResource("com/intellij") != null;
    }

}
