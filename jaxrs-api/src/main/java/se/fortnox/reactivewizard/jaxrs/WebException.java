package se.fortnox.reactivewizard.jaxrs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.UUID;

@SuppressWarnings("serial")
@JsonIgnoreProperties({ "cause", "stackTrace", "localizedMessage", "suppressed" })
public class WebException extends RuntimeException {

	private String             id;
	private String             error;
	private FieldError[]       fields;
	private Object[]           errorParams;
	private HttpResponseStatus status;
	private String             message;

	public WebException(HttpResponseStatus httpStatus) {
		this(httpStatus, (Throwable) null);
	}

	public WebException(HttpResponseStatus httpStatus, Throwable e) {
		this(httpStatus, e, true);
	}

	public WebException(HttpResponseStatus httpStatus, Throwable e, boolean stacktrace) {
		super(null, e, false, stacktrace);
		this.error = errCodeFromStatus(httpStatus);
		this.status = httpStatus;
		this.id = UUID.randomUUID().toString();
	}

	public WebException(HttpResponseStatus httpStatus, String errorCode) {
		this(httpStatus, errorCode, null);
	}

	public WebException(HttpResponseStatus httpStatus, FieldError... fieldErrors) {
		this.status = httpStatus;
		if (fieldErrors != null && fieldErrors.length != 0) {
			this.fields = fieldErrors;
			error = "validation";
		} else {
			error = errCodeFromStatus(httpStatus);
		}
		this.id = UUID.randomUUID().toString();
	}

	public WebException(HttpResponseStatus httpStatus, String errorCode, String userMessage) {
		this.message = userMessage;
		this.status = httpStatus;
		this.error = errorCode;
		this.id = UUID.randomUUID().toString();
	}

	private static String errCodeFromStatus(HttpResponseStatus status) {
		if (status.equals(HttpResponseStatus.INTERNAL_SERVER_ERROR)) {
			return "internal";
		}
		return status.reasonPhrase().toLowerCase().replaceAll(" ", "");
	}

	public WebException(FieldError... fieldErrors) {
		this(HttpResponseStatus.BAD_REQUEST, fieldErrors);
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
}
