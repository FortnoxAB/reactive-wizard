package se.fortnox.reactivewizard.jaxrs.params.deserializing;

public class DeserializerException extends Exception {
    public DeserializerException(String errorCode) {
        super(errorCode);
    }
}
