package se.fortnox.reactivewizard.json;

public class InvalidJsonException extends RuntimeException {
    public InvalidJsonException(Exception exception) {
        super(exception);
    }

    @Override
    public String getMessage() {
        String[] lines = getCause().getMessage().split("\n");
        return lines[0];
    }
}
