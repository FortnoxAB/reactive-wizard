package se.fortnox.reactivewizard.db.statement;

import rx.Subscriber;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.sql.SQLException;

public class UpdateStatementReturningVoidFactory extends UpdateStatementExecutorReturningCountFactory {

    public UpdateStatementReturningVoidFactory(ParameterizedQuery parameterizedQuery, int minimumAffected) {
        super(parameterizedQuery, minimumAffected);
    }

    @Override
    protected void executed(int count, Subscriber subscriber) throws SQLException {
        ensureMinimumReached(count);
    }
}
