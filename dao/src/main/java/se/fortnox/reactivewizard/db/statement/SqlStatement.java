package se.fortnox.reactivewizard.db.statement;

import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;
import se.fortnox.reactivewizard.db.query.ParamSetter;
import se.fortnox.reactivewizard.db.query.PreparedStatementParameters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public abstract class SqlStatement<T> implements Statement {

    protected final String sql;

    private final List<ParamSetter> paramSetters;

    protected FluxSink<T> fluxSink;

    protected MonoSink<T> monoSink;

    protected SqlStatement(String sql, List<ParamSetter> paramSetters) {
        this.sql = sql;
        this.paramSetters = paramSetters;
    }

    protected PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement(sql);
    }

    protected PreparedStatement createPreparedStatementWithGeneratedKeys(Connection connection) throws SQLException {
        return connection.prepareStatement(sql, RETURN_GENERATED_KEYS);
    }

    protected void addParameters(PreparedStatement preparedStatement) throws SQLException {
        var parameters = new PreparedStatementParameters(preparedStatement);
        for (var paramSetter : paramSetters) {
            paramSetter.call(parameters);
        }
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
        this.fluxSink = (FluxSink<T>) fluxSink;
    }

    @Override
    public void setMonoSink(MonoSink<?> monoSink) {
        if (fluxSink != null) {
            throw new UnsupportedOperationException("Cannot set MonoSink if FluxSink is set");
        }
        this.monoSink = (MonoSink<T>) monoSink;
    }
}
