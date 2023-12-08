package se.fortnox.reactivewizard.jaxrs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.event.Level;

import java.util.UUID;

@SuppressWarnings("serial")
@JsonIgnoreProperties({"cause", "stackTrace", "localizedMessage", "suppressed", "logLevel", "body"})
public class WebException extends RuntimeException {

    private   String             id;
    private   String             error;
    private   FieldError[]       fields;
    private   Object[]           errorParams;
    private   HttpResponseStatus status;
    private   String             message;
    protected Level              logLevel;


    private final String body;


    public WebException(HttpResponseStatus httpStatus, Throwable throwable, boolean stacktrace, String body) {
        super(null, throwable, false, stacktrace);
        this.error = errorCodeFromStatus(httpStatus);
        this.status = httpStatus;
        this.id = createUUID();
        this.logLevel = logLevelFromStatus(httpStatus);
        this.body = body;
    }

    public WebException(HttpResponseStatus httpStatus, Throwable throwable, boolean stacktrace) {
        super(null, throwable, false, stacktrace);
        this.error = errorCodeFromStatus(httpStatus);
        this.status = httpStatus;
        this.id = createUUID();
        this.logLevel = logLevelFromStatus(httpStatus);
        this.body = null;
    }

    public WebException(HttpResponseStatus httpStatus, String errorCode, Throwable throwable) {
        this(httpStatus, throwable, true);
        this.error = errorCode;
    }

    public WebException(HttpResponseStatus httpStatus) {
        this(httpStatus, (Throwable)null);
    }

    public WebException(HttpResponseStatus httpStatus, Throwable throwable) {
        this(httpStatus, throwable, true);
    }

    public WebException(HttpResponseStatus httpStatus, FieldError... fieldErrors) {
        this.status = httpStatus;
        if (fieldErrors != null && fieldErrors.length != 0) {
            this.fields = fieldErrors;
            error = "validation";
        } else {
            error = errorCodeFromStatus(httpStatus);
        }
        this.id = createUUID();
        this.logLevel = logLevelFromStatus(httpStatus);
        this.body = null;
    }

    public WebException(FieldError... fieldErrors) {
        this(HttpResponseStatus.BAD_REQUEST, fieldErrors);

        //Since this exception is thrown due to one or many field errors we set the log level to INFO
        this.logLevel = Level.INFO;
    }

    public WebException(HttpResponseStatus httpStatus, String errorCode, String userMessage) {
        this.message = userMessage;
        this.status = httpStatus;
        this.error = errorCode;
        this.id = createUUID();
        this.logLevel = logLevelFromStatus(httpStatus);
        this.body = null;
    }

    public WebException(HttpResponseStatus httpStatus, String errorCode) {
        this(httpStatus, errorCode, (String)null);
    }

    private static String errorCodeFromStatus(HttpResponseStatus status) {
        if (status.equals(HttpResponseStatus.INTERNAL_SERVER_ERROR)) {
            return "internal";
        }
        return status.reasonPhrase().toLowerCase().replaceAll(" ", "");
    }

    @JsonIgnore
    public HttpResponseStatus getStatus() {
        return status;
    }

    public WebException withErrorParams(Object... params) {
        setErrorParams(params);
        return this;
    }

    public String getId() {
        return id;
    }

    public String getError() {
        return error;
    }

    @JsonInclude(Include.NON_NULL)
    public FieldError[] getFields() {
        return fields;
    }

    @Override
    @JsonInclude(Include.NON_NULL)
    public String getMessage() {
        return message;
    }

    @JsonInclude(Include.NON_NULL)
    public Object[] getErrorParams() {
        return errorParams;
    }

    public void setErrorParams(Object[] errorParams) {
        this.errorParams = errorParams;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public WebException withLogLevel(Level logLevel) {
        setLogLevel(logLevel);
        return this;
    }

    public String getBody() {
        return body;
    }

    private Level logLevelFromStatus(HttpResponseStatus httpStatus) {
        if (httpStatus.code() >= 500) {
            return Level.ERROR;

        } else if (httpStatus.equals(HttpResponseStatus.NOT_FOUND)) {
            return Level.DEBUG;
        }

        return Level.WARN;
    }

    private String createUUID() {
        return UUID.randomUUID().toString();
    }
}
