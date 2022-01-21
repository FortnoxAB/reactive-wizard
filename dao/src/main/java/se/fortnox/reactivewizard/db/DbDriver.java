package se.fortnox.reactivewizard.db;

public final class DbDriver {
    private DbDriver() {

    }

    /**
     * Load driver.
     * @param url the driver url
     */
    public static void loadDriver(String url) {
        if (url.startsWith("jdbc:h2:")) {
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Error loading database driver", e);
            }
        } else if (url.startsWith("jdbc:mysql:")) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Error loading database driver", e);
            }
        }
    }
}
