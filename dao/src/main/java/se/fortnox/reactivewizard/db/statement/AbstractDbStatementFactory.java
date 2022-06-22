package se.fortnox.reactivewizard.db.statement;

import reactor.core.publisher.FluxSink;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractDbStatementFactory implements DbStatementFactory {
    protected final ParameterizedQuery parameterizedQuery;

    protected AbstractDbStatementFactory(ParameterizedQuery parameterizedQuery) {
        this.parameterizedQuery = parameterizedQuery;
    }

    protected abstract void executeStatement(Connection connection, Object[] args, FluxSink<?> fluxSink)
            throws SQLException;

    protected PreparedStatement batch(Connection connection, PreparedStatement preparedStatement, Object[] args) throws SQLException {
        throw new UnsupportedOperationException();
    }

    protected void batchExecuted(int count, FluxSink<?> fluxSink) throws SQLException {
        throw new UnsupportedOperationException();
    }

    protected boolean sameBatch(ParameterizedQuery parameterizedQuery) {
        return false;
    }

    @Override
    public Statement create(Object[] args) {
        return new StatementImpl(args, parameterizedQuery);
    }

    private class StatementImpl implements Statement {

        private final Object[] args;
        private final ParameterizedQuery parameterizedQuery;
        private FluxSink<?> fluxSink;

        private StatementImpl(Object[] args, ParameterizedQuery parameterizedQuery) {
            this.args = args;
            this.parameterizedQuery = parameterizedQuery;
        }

        @Override
        public void execute(Connection connection) throws SQLException {
            executeStatement(connection, args, fluxSink);
        }

        @Override
        public PreparedStatement batch(Connection connection, PreparedStatement preparedStatement) throws SQLException {
            return AbstractDbStatementFactory.this.batch(connection, preparedStatement, args);
        }

        @Override
        public void batchExecuted(int count) throws SQLException {
            AbstractDbStatementFactory.this.batchExecuted(count, fluxSink);
        }

        @Override
        public boolean sameBatch(Statement statement) {
            if (!(statement instanceof StatementImpl statementImpl)) {
                return false;
            }
            return AbstractDbStatementFactory.this.sameBatch(statementImpl.parameterizedQuery);
        }

        @Override
        public void onCompleted() {
            if (fluxSink != null) {
                fluxSink.complete();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (fluxSink != null) {
                fluxSink.error(throwable);
            }
        }

        @Override
        public void setFluxSink(FluxSink<?> fluxSink) {
            this.fluxSink = fluxSink;
        }
    }

}
