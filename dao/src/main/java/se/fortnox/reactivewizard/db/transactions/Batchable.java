package se.fortnox.reactivewizard.db.transactions;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Batchable statement.
 */
public interface Batchable {
    /**
     * Check if this Batchable can be run in the same batch.
     * @param batchable Another batchable.
     * @return <code>true</code> if this instance may be run in same batch as batchable.
     */
    boolean sameBatch(Batchable batchable);

    /**
     * Execute the batch.
     */
    void execute(Connection connection) throws SQLException;
}
