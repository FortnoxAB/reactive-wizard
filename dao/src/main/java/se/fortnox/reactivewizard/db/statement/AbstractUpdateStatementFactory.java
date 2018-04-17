package se.fortnox.reactivewizard.db.statement;

import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.sql.SQLException;

public abstract class AbstractUpdateStatementFactory extends AbstractDbStatementFactory {
    private final int minimumAffected;

    public AbstractUpdateStatementFactory(int minimumAffected, ParameterizedQuery parameterizedQuery) {
        super(parameterizedQuery);
        this.minimumAffected = minimumAffected;
    }

    protected void ensureMinimumReached(int updateCount) throws SQLException {
        if (updateCount < minimumAffected) {
            throw new MinimumAffectedRowsException(minimumAffected, updateCount, toString());
        }
    }

    @Override
    public String toString() {
        return parameterizedQuery.toString();
    }
}
