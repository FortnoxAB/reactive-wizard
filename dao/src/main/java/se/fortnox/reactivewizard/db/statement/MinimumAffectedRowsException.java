package se.fortnox.reactivewizard.db.statement;

import java.sql.SQLException;

public class MinimumAffectedRowsException extends SQLException {

    public MinimumAffectedRowsException(int minimumAffected, int updateCount, String query) {
        super("Minimum affected rows not reached for query \"" + query + "\". \n"
            + "Minimum: " + minimumAffected + " actual: " + updateCount);
    }
}
