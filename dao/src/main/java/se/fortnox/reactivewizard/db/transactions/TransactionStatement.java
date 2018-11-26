package se.fortnox.reactivewizard.db.transactions;

import se.fortnox.reactivewizard.db.ConnectionProvider;
import se.fortnox.reactivewizard.db.statement.Statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

public class TransactionStatement implements Batchable {
	private final AtomicReference<Statement> statement;
	private final Transaction                transaction;

	public TransactionStatement(Transaction transaction) {
		this.transaction = transaction;
		this.statement = new AtomicReference<>();
	}

	public boolean isSubscribed() {
		return statement.get() != null;
	}

	public void executeStatement(Statement statement) {
		if (this.statement.compareAndSet(null, statement)) {
			transaction.subscribed();
		} else {
			throw new TransactionAlreadyExecutedException();
		}
	}

	public Statement getStatement() {
		return statement.get();
	}

	public void removeStatement() {
		statement.set(null);
	}

	public void setConnectionProvider(ConnectionProvider connectionProvider) {
		transaction.setConnectionProvider(connectionProvider);
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public AtomicReference<Statement> getAtomicStatement() {
		return statement;
	}

	@Override
	public boolean sameBatch(Batchable batchable) {
		return batchable instanceof TransactionStatement
			&& getStatement().sameBatch(((TransactionStatement)batchable).getStatement());
	}

	@Override
	public void execute(Connection connection) throws SQLException {
		getStatement().execute(connection);
	}
}
