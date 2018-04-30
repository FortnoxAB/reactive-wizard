package se.fortnox.reactivewizard.db.transactions;

public class TransactionAlreadyExecutedException extends RuntimeException {
    public TransactionAlreadyExecutedException() {
        super("Transaction already executed. You cannot subscribe multiple times to an " +
            "Observable that is part of a transaction.");
    }
}
