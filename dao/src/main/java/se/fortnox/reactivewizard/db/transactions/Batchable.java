package se.fortnox.reactivewizard.db.transactions;

import java.sql.Connection;
import java.sql.SQLException;

public interface Batchable {
    /**
     * @param batchable Another batchable.
     * @return <code>true</code> if this instance may be ran in same batch as batchable.
     */
    boolean sameBatch(Batchable batchable);

    /**
     * Execute the batch.
     */
    void execute(Connection connection) throws SQLException;
}
