package se.fortnox.reactivewizard.db.statement;

import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;
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

    protected abstract void executeStatement(Connection connection, Object[] args, MonoSink<?> monoSink)
        throws SQLException;

    protected PreparedStatement batch(Connection connection, PreparedStatement preparedStatement, Object[] args) throws SQLException {
        throw new UnsupportedOperationException();
    }

    protected void batchExecuted(int count, FluxSink<?> fluxSink) throws SQLException {
        throw new UnsupportedOperationException();
    }

    protected void batchExecuted(int count, MonoSink<?> monoSink) {
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
        private MonoSink<?> monoSink;

        private StatementImpl(Object[] args, ParameterizedQuery parameterizedQuery) {
            this.args = args;
            this.parameterizedQuery = parameterizedQuery;
        }

        @Override
        public void execute(Connection connection) throws SQLException {
            if (fluxSink != null) {
                executeStatement(connection, args, fluxSink);
            } else {
                executeStatement(connection, args, monoSink);
            }
        }

        @Override
        public PreparedStatement batch(Connection connection, PreparedStatement preparedStatement) throws SQLException {
            return AbstractDbStatementFactory.this.batch(connection, preparedStatement, args);
        }

        @Override
        public void batchExecuted(int count) throws SQLException {
            if (monoSink != null) {
                AbstractDbStatementFactory.this.batchExecuted(count, monoSink);
            } else {
                AbstractDbStatementFactory.this.batchExecuted(count, fluxSink);
            }
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
            if (monoSink != null) {
                monoSink.error(throwable);
            }

        }

        @Override
        public void setFluxSink(FluxSink<?> fluxSink) {
            if (monoSink != null) {
                throw new UnsupportedOperationException("Cannot set FluxSink if MonoSink is set");
            }
            this.fluxSink = fluxSink;
        }

        @Override
        public void setMonoSink(MonoSink<?> monoSink) {
            if (fluxSink != null) {
                throw new UnsupportedOperationException("Cannot set MonoSink if FluxSink is set");
            }
            this.monoSink = monoSink;
        }
    }

}
