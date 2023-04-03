package se.fortnox.reactivewizard.db.statement;

import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public interface Statement {
    void execute(Connection connection) throws SQLException;

    void onCompleted();

    void onError(Throwable throwable);

    /**
     * Adds a batch update to the prepared statement. If no prepared statement is supplied the method will create one
     * and return it.
     *
     * @param connection        The connection.
     * @param preparedStatement The prepared statement to add the batch to.
     * @return The prepared statement sent in, or a new based on the query that will be used for the upcoming batch.
     * @throws SQLException If there's any SQL error.
     */
    PreparedStatement batch(Connection connection, PreparedStatement preparedStatement) throws SQLException;

    /**
     * Must be called after the batch has been executed with the update count for this specific statement. Not the whole
     * batch.
     *
     * @param count The update count for this statement.
     * @throws SQLException If there's any SQL error.
     */
    void batchExecuted(int count) throws SQLException;

    /**
     * Checks compatibility with other batch.
     *
     * @return <code>true</code> if the specified statement might be added to the batch.
     */
    boolean sameBatch(Statement statement);

    /**
     * Set the FluxSink.
     * Cannot be set if MonoSink is already set.
     *
     * @param fluxSink the FluxSink
     */
    void setFluxSink(FluxSink<?> fluxSink);

    /**
     * Set the MonoSink.
     * Cannot be set if FluxSink is already set.
     *
     * @param monoSink the MonoSink
     */
    void setMonoSink(MonoSink<?> monoSink);
}
