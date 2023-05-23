package se.fortnox.reactivewizard.db.statement;

import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.sql.SQLException;

public class UpdateStatementReturningVoidFactory extends UpdateStatementExecutorReturningCountFactory {

    public UpdateStatementReturningVoidFactory(ParameterizedQuery parameterizedQuery, int minimumAffected) {
        super(parameterizedQuery, minimumAffected);
    }

    @Override
    protected void executed(int count, FluxSink fluxSink) throws SQLException {
        ensureMinimumReached(count);
    }

    @Override
    protected void executed(int count, MonoSink monoSink) throws SQLException {
        ensureMinimumReached(count);
        if (monoSink != null) {
            monoSink.success();
        }
    }
}
