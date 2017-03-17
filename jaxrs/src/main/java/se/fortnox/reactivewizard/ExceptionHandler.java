package se.fortnox.reactivewizard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.json.InvalidJsonException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.exceptions.CompositeException;
import rx.exceptions.OnErrorThrowable;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.nio.channels.ClosedChannelException;
import java.nio.file.FileSystemException;
import java.util.List;

/**
 * Handles exceptions and writes errors to the response and the log.
 */
public class ExceptionHandler {

	private static Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);
	private ObjectMapper mapper;

	@Inject
	public ExceptionHandler(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public ExceptionHandler() {
		this(new ObjectMapper());
	}

	public Observable<Void> handleException(HttpServerRequest<ByteBuf> request,
			HttpServerResponse<ByteBuf> response, Throwable e) {
		if (e instanceof OnErrorThrowable) {
			e = e.getCause();
		}

		if (e instanceof CompositeException) {
			CompositeException ce = (CompositeException) e;
			List<Throwable> exceptions = ce.getExceptions();
			e = exceptions.get(exceptions.size() - 1);
		}

		WebException we;
		if (e instanceof FileSystemException) {
			we = new WebException(HttpResponseStatus.NOT_FOUND);
		} else if (e instanceof InvalidJsonException) {
			we = new WebException(HttpResponseStatus.BAD_REQUEST, "invalidjson", e.getMessage());
		} else if (e instanceof WebException) {
			we = (WebException) e;
		} else if (e instanceof ClosedChannelException) {
			LOG.debug("ClosedChannelException: " + request.getHttpMethod() + " " + request.getUri(), e);
			return response.close();
		} else {
			we = new WebException(HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
		}

		if (we.getStatus().code() >= 500) {
			LOG.error(getLogMessage(request, we), we);
		} else {
			if (we.getStatus() != HttpResponseStatus.NOT_FOUND) {
				// No log for 404
				LOG.warn(getLogMessage(request, we), we);
			}
		}

		response.setStatus(we.getStatus());
		response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON);
		response.writeString(json(we));
		return response.close();
	}

	private String json(WebException we) {
		try {
			return mapper.writeValueAsString(we);
		} catch (JsonProcessingException e) {
			LOG.error("Error writing json for exception "+we, e);
			return null;
		}
	}

	private String getLogMessage(HttpServerRequest<ByteBuf> request,
			WebException webException) {
		return new StringBuilder()
				.append(webException.getStatus().toString())
				.append("\n\tCause: ").append(webException.getCause() != null ?
						webException.getCause().getMessage() :
						"-")
				.append("\n\tResponse: ").append(json(webException))
				.append("\n\tRequest: ")
				.append(request.getHttpMethod())
				.append(" ").append(request.getUri())
				.append(" headers: ").append(
						request.getHeaders() != null ? request.getHeaders().entries() : "[]").toString();
	}

}
