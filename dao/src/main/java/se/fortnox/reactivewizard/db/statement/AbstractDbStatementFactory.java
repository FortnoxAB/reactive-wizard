package se.fortnox.reactivewizard.db.statement;

import rx.Subscriber;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractDbStatementFactory implements DbStatementFactory {
    protected final ParameterizedQuery parameterizedQuery;

    protected AbstractDbStatementFactory(ParameterizedQuery parameterizedQuery) {
        this.parameterizedQuery = parameterizedQuery;
    }

    protected abstract void executeStatement(Connection connection, Object[] args, Subscriber subscriber)
        throws SQLException;

    protected PreparedStatement batch(Connection connection, PreparedStatement preparedStatement, Object[] args) throws SQLException {
        throw new UnsupportedOperationException();
    }

    protected void batchExecuted(int count, Subscriber subscriber) throws SQLException {
        throw new UnsupportedOperationException();
    }

    protected boolean sameBatch(ParameterizedQuery parameterizedQuery) {
        return false;
    }

    @Override
    public Statement create(Object[] args, Subscriber subscriber) {
        return new StatementImpl(args, subscriber, parameterizedQuery);
    }

    private class StatementImpl implements Statement {

        private final Object[]           args;
        private final Subscriber         subscriber;
        private final ParameterizedQuery parameterizedQuery;

        private StatementImpl(Object[] args, Subscriber subscriber, ParameterizedQuery parameterizedQuery) {
            this.args = args;
            this.subscriber = subscriber;
            this.parameterizedQuery = parameterizedQuery;
        }

        @Override
        public void execute(Connection connection) throws SQLException {
            executeStatement(connection, args, subscriber);
        }

        @Override
        public PreparedStatement batch(Connection connection, PreparedStatement preparedStatement) throws SQLException {
            return AbstractDbStatementFactory.this.batch(connection, preparedStatement, args);
        }

        @Override
        public void batchExecuted(int count) throws SQLException {
            AbstractDbStatementFactory.this.batchExecuted(count, subscriber);
        }

        @Override
        public boolean sameBatch(Statement statement) {
            if (!(statement instanceof StatementImpl)) {
                return false;
            }

            return AbstractDbStatementFactory.this.sameBatch(((StatementImpl)statement).parameterizedQuery);
        }

        @Override
        public void onCompleted() {
            subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable throwable) {
            subscriber.onError(throwable);
        }
    }

}
