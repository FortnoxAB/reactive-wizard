package se.fortnox.reactivewizard;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.reactivex.netty.channel.AllocatingTransformer;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.ResponseContentWriter;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import io.reactivex.netty.protocol.http.ws.server.WebSocketHandler;
import io.reactivex.netty.protocol.http.ws.server.WebSocketHandshaker;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import java.text.ParseException;
import java.util.*;

public class MockHttpServerResponse extends HttpServerResponse<ByteBuf> {

	private StringBuilder outp = new StringBuilder();
	private HttpResponseStatus status;
	private Map<CharSequence,Object> headers = new HashMap<>();

	public MockHttpServerResponse() {
		super(new OnSubscribe<Void>() {
			@Override
			public void call(Subscriber<? super Void> subscriber) {
				subscriber.onCompleted();
			}
		});
	}

	public String getOutp() {
		return outp.toString();
	}

	@Override
	public HttpResponseStatus getStatus() {
		return status;
	}

	@Override
	public boolean containsHeader(CharSequence charSequence) {
		return false;
	}

	@Override
	public boolean containsHeader(CharSequence charSequence, CharSequence charSequence1, boolean b) {
		return false;
	}

	@Override
	public String getHeader(CharSequence charSequence) {
		Object header = headers.get(charSequence);
		if (header == null) {
			return null;
		}
		return header.toString();
	}

	@Override
	public String getHeader(CharSequence charSequence, String s) {
		return null;
	}

	@Override
	public List<String> getAllHeaderValues(CharSequence charSequence) {
		return null;
	}

	@Override
	public long getDateHeader(CharSequence charSequence) throws ParseException {
		return 0;
	}

	@Override
	public long getDateHeader(CharSequence charSequence, long l) {
		return 0;
	}

	@Override
	public int getIntHeader(CharSequence charSequence) {
		return 0;
	}

	@Override
	public int getIntHeader(CharSequence charSequence, int i) {
		return 0;
	}

	@Override
	public Set<String> getHeaderNames() {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> addHeader(CharSequence key, Object value) {
		headers.put(key, value);
		return this;
	}

	@Override
	public HttpServerResponse<ByteBuf> addCookie(Cookie cookie) {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> addDateHeader(CharSequence charSequence, Date date) {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> addDateHeader(CharSequence charSequence, Iterable<Date> iterable) {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> addHeader(CharSequence charSequence, Iterable<Object> iterable) {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> setDateHeader(CharSequence charSequence, Date date) {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> setHeader(CharSequence charSequence, Object o) {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> setDateHeader(CharSequence charSequence, Iterable<Date> iterable) {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> setHeader(CharSequence charSequence, Iterable<Object> iterable) {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> removeHeader(CharSequence charSequence) {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> setStatus(HttpResponseStatus httpResponseStatus) {
		status = httpResponseStatus;
		return this;
	}

	@Override
	public HttpServerResponse<ByteBuf> setTransferEncodingChunked() {
		return null;
	}

	@Override
	public HttpServerResponse<ByteBuf> flushOnlyOnReadComplete() {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> sendHeaders() {
		return null;
	}

	@Override
	public HttpServerResponse<ServerSentEvent> transformToServerSentEvents() {
		return null;
	}

	@Override
	public <CC> HttpServerResponse<CC> transformContent(AllocatingTransformer<CC, ByteBuf> allocatingTransformer) {
		return null;
	}

	@Override
	public WebSocketHandshaker acceptWebSocketUpgrade(WebSocketHandler webSocketHandler) {
		return null;
	}

	@Override
	public Observable<Void> dispose() {
		return null;
	}

	@Override
	public Channel unsafeNettyChannel() {
		return null;
	}

	@Override
	public Connection<?, ?> unsafeConnection() {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> write(Observable<ByteBuf> observable) {
		return null;
	}

	@Override
	public <T extends TrailingHeaders> Observable<Void> write(Observable<ByteBuf> observable, Func0<T> func0, Func2<T, ByteBuf, T> func2) {
		return null;
	}

	@Override
	public <T extends TrailingHeaders> Observable<Void> write(Observable<ByteBuf> observable, Func0<T> func0, Func2<T, ByteBuf, T> func2, Func1<ByteBuf, Boolean> func1) {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> write(Observable<ByteBuf> observable, Func1<ByteBuf, Boolean> func1) {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> writeAndFlushOnEach(Observable<ByteBuf> observable) {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> writeString(Observable<String> observable) {
		observable.toBlocking().forEach(str->outp.append(str));
		return this;
	}

	@Override
	public <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> observable, Func0<T> func0, Func2<T, String, T> func2) {
		return null;
	}

	@Override
	public <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> observable, Func0<T> func0, Func2<T, String, T> func2, Func1<String, Boolean> func1) {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> writeString(Observable<String> observable, Func1<String, Boolean> func1) {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> writeStringAndFlushOnEach(Observable<String> observable) {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> writeBytes(Observable<byte[]> observable) {
		observable.toBlocking().forEach(bytes->outp.append(new String(bytes)));
		return this;
	}

	@Override
	public <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> observable, Func0<T> func0, Func2<T, byte[], T> func2) {
		return null;
	}

	@Override
	public <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> observable, Func0<T> func0, Func2<T, byte[], T> func2, Func1<byte[], Boolean> func1) {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> writeBytes(Observable<byte[]> observable, Func1<byte[], Boolean> func1) {
		return null;
	}

	@Override
	public ResponseContentWriter<ByteBuf> writeBytesAndFlushOnEach(Observable<byte[]> observable) {
		return writeBytes(observable);
	}
}
