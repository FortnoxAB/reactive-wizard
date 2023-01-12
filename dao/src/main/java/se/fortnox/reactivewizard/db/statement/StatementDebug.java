package se.fortnox.reactivewizard.db.statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;

public class StatementDebug {
    private static final Logger LOG = LoggerFactory.getLogger(StatementDebug.class);
    static boolean ENABLE_STATEMENT_DEBUG = Boolean.parseBoolean(System.getenv("dao.debug"));

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
