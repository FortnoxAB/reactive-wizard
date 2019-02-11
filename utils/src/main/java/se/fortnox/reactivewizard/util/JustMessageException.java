package se.fortnox.reactivewizard.util;

public class JustMessageException extends Exception {
    public JustMessageException(String message) {
        this(message, null);
    }

    public JustMessageException(String message, Throwable cause) {
        super(message, cause, false, false);
    }
}
