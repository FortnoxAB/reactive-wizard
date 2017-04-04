package se.fortnox.reactivewizard.util;

public class DebugUtil {
    public static final boolean IS_DEBUG = isIDEPresent();

    private static boolean isIDEPresent() {
        ClassLoader classLoader = DebugUtil.class.getClassLoader();
        boolean intellijPresent = classLoader.getResource("com/intellij") != null;
        return intellijPresent;
    }

}
