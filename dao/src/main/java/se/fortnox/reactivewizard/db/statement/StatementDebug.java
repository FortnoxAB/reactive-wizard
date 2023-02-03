package se.fortnox.reactivewizard.db.statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;

public class StatementDebug {
    private static final Logger LOG = LoggerFactory.getLogger(StatementDebug.class);
    private static final boolean ENABLE_STATEMENT_DEBUG = parseBoolean(getProperty("dao.debug"))
        && "dev".equals(getenv("ENV_CONTEXT"));

    private StatementDebug() {
    }

    /**
     * Logging statement if dao.debug is set to true.
     * This only works if an actual postgres database
     * is running.
     * @param statement a sql PreparedStatement
     */
    public static void log(PreparedStatement statement) {
        if (ENABLE_STATEMENT_DEBUG) {
            LOG.info("Executed statement: {}", statement);
        }
    }

}
