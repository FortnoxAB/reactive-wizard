package se.fortnox.reactivewizard.dbmigrate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import liquibase.exception.LiquibaseException;
import se.fortnox.reactivewizard.config.ConfigFactory;

import java.io.IOException;

@Singleton
public class LiquibaseMigrateProvider {
    private LiquibaseMigrate liquibaseMigrate;
    private final LiquibaseConfig liquibaseConfig;

    @Inject
    public LiquibaseMigrateProvider(ConfigFactory configFactory) {
        liquibaseConfig = configFactory.get(LiquibaseConfig.class);
    }

    public LiquibaseMigrate get() {
        if (liquibaseMigrate == null) {
            try {
                liquibaseMigrate = new LiquibaseMigrate(liquibaseConfig);
            } catch (LiquibaseException | IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return liquibaseMigrate;
    }
}
