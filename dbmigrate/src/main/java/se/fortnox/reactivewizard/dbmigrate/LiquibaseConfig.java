package se.fortnox.reactivewizard.dbmigrate;

import se.fortnox.reactivewizard.config.Config;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;

/**
 * Configure the database connection to use for liquibase migrations. This is usually the same configuration as for
 * "database" but you should use a different user, so that normal CRUD operations are not run with a user having
 * privileges to do CREATE/ALTER/DROP.
 * <p>
 * Note that if you have multiple modules with migrations, you should make the fatjar (shade) with a single merged migrations.xml:
 *
 * <pre>{@code
 * <transformers>
 *    <transformer implementation="org.apache.maven.plugins.shade.resource.XmlAppendingTransformer">
 *      <resource>migrations.xml</resource>
 *    </transformer>
 * </transformers>
 * }</pre>
 */
@Config("liquibase-database")
public class LiquibaseConfig extends DatabaseConfig {
    private String migrationsFile = "migrations.xml";

    public String getMigrationsFile() {
        return migrationsFile;
    }

    public void setMigrationsFile(String migrationsFile) {
        this.migrationsFile = migrationsFile;
    }
}
