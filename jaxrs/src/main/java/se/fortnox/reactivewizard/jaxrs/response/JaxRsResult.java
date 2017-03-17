package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.HashMap;
import java.util.Map;

import static se.fortnox.reactivewizard.util.rx.RxUtils.doIfEmpty;
import static rx.Observable.empty;

/**
 * Represents a result of a call to a JaxRs resource. Contains the output but also some meta data about the call.
 * This class is passed to the output processors.
 */
public class JaxRsResult<T> {

	private Observable<T>	output;
	private HttpResponseStatus responseStatus;
	private final Func1<T,byte[]> serializer;
	private final Map<String,Object> headers = new HashMap<>();

	public JaxRsResult(Observable<T> output,
					   HttpResponseStatus responseStatus,
					   Func1<T,byte[]> serializer,
					   Map<String, Object> headers
					   ) {
		this.output = setStatusForNoContent(output);
		this.responseStatus = responseStatus;
		this.serializer = serializer;
		this.headers.putAll(headers);
	}

	private Observable<T> setStatusForNoContent(Observable<T> output) {
		return doIfEmpty(output, ()->responseStatus = HttpResponseStatus.NO_CONTENT);
	}

	public JaxRsResult<T> addHeader(String key, Object val) {
		headers.put(key, val);
		return this;
	}

	public JaxRsResult<T> doOnOutput(Action1<T> action) {
		output = output.doOnNext(action);
		return this;
	}

	public Observable<Void> write(HttpServerResponse<ByteBuf> response) {
		return output.map(serializer).defaultIfEmpty(null).flatMap(bytes->{
			response.setStatus(responseStatus);
			headers.forEach((key,val)->response.getHeaders().add(key, val));
			return writeBody(response, bytes);
		});
	}

	protected Observable<Void> writeBody(HttpServerResponse<ByteBuf> response, byte[] bytes) {
		if (bytes != null) {
			response.getHeaders().add("Content-Length", bytes.length);
			return response.writeBytesAndFlush(bytes);
		}
		return empty();
	}
}
