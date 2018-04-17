package se.fortnox.reactivewizard.db.statement;

import rx.Subscriber;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateStatementExecutorReturningCountFactory extends AbstractUpdateStatementFactory {

    public UpdateStatementExecutorReturningCountFactory(ParameterizedQuery parameterizedQuery, int minimumAffected) {
        super(minimumAffected, parameterizedQuery);
    }

    @Override
    protected void executeStatement(Connection connection, Object[] args, Subscriber subscriber)
        throws SQLException {
        try (PreparedStatement statement = parameterizedQuery.createStatement(connection, args)) {
            parameterizedQuery.addParameters(args, statement);
            executed(statement.executeUpdate(), subscriber);
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

    protected void executed(int count, Subscriber subscriber) throws SQLException {
        ensureMinimumReached(count);
        subscriber.onNext(count);
    }

    @Override
    protected void batchExecuted(int count, Subscriber subscriber) throws SQLException {
        executed(count, subscriber);
    }

    @Override
    protected boolean sameBatch(ParameterizedQuery parameterizedQuery) {
        return parameterizedQuery.toString().equals(this.parameterizedQuery.toString());
    }
}
