package se.fortnox.reactivewizard.db.statement;

import reactor.core.publisher.FluxSink;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateStatementExecutorReturningCountFactory extends AbstractUpdateStatementFactory {

    public UpdateStatementExecutorReturningCountFactory(ParameterizedQuery parameterizedQuery, int minimumAffected) {
        super(minimumAffected, parameterizedQuery);
    }

    @Override
    protected void executeStatement(Connection connection, Object[] args, FluxSink fluxSink)
            throws SQLException {
        try (PreparedStatement statement = parameterizedQuery.createStatement(connection, args)) {
            parameterizedQuery.addParameters(args, statement);
            executed(statement.executeUpdate(), fluxSink);
            StatementDebug.log(statement);
        }
    }

    @Override
    protected PreparedStatement batch(Connection connection, PreparedStatement preparedStatement, Object[] args) throws SQLException {
        if (preparedStatement == null) {
            preparedStatement = parameterizedQuery.createStatement(connection, args);
        }

        parameterizedQuery.addParameters(args, preparedStatement);
        preparedStatement.addBatch();
        return preparedStatement;
    }

    protected void executed(int count, FluxSink fluxSink) throws SQLException {
        ensureMinimumReached(count);
        if (fluxSink != null) {
            fluxSink.next(count);
        }
    }

    @Override
    protected void batchExecuted(int count, FluxSink fluxSink) throws SQLException {
        executed(count, fluxSink);
    }

    @Override
    protected boolean sameBatch(ParameterizedQuery parameterizedQuery) {
        return parameterizedQuery.toString().equals(this.parameterizedQuery.toString());
    }
}
