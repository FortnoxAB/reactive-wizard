package se.fortnox.reactivewizard.dbmigrate;

import com.google.inject.Inject;
import liquibase.exception.LiquibaseException;
import se.fortnox.reactivewizard.config.ConfigFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class LiquibaseMigrateProvider {
    private final AtomicReference<LiquibaseMigrate> liquibaseMigrate = new AtomicReference<>();
    private final LiquibaseConfig liquibaseConfig;

    @Inject
    public LiquibaseMigrateProvider(ConfigFactory configFactory) {
        liquibaseConfig = configFactory.get(LiquibaseConfig.class);
    }

    public LiquibaseMigrate get() {
        if (liquibaseMigrate.get() == null) {
            try {
                liquibaseMigrate.set(new LiquibaseMigrate(liquibaseConfig));
            } catch (LiquibaseException | IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return liquibaseMigrate.get();
    }
}
