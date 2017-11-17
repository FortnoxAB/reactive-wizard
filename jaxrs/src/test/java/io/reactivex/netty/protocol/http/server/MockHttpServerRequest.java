package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.internal.HttpContentSubscriberEvent;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.UnicastSubject;

import java.util.List;
import java.util.Map;

public class MockHttpServerRequest extends HttpServerRequestImpl<ByteBuf> {

	private DefaultHttpRequest					req;
	private Map<String, List<String>>	query;

	protected MockHttpServerRequest(Channel channel, DefaultHttpRequest nettyRequest,
									Observable<ByteBuf> content) {
		super(nettyRequest, setupChannel(channel, content));
		this.req = nettyRequest;
	}

	private static Channel setupChannel(Channel channel, Observable<ByteBuf> content) {
		if (channel != null) {
			return channel;
		}
		return new EmbeddedChannel(new ChannelDuplexHandler(){
			@Override
			public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
				if (evt instanceof HttpContentSubscriberEvent) {
					Subscriber subscriber = ((HttpContentSubscriberEvent) evt).getSubscriber();
					content.subscribe(subscriber);
				}
				super.userEventTriggered(ctx, evt);
			}
		});
	}

	public MockHttpServerRequest(String uri) {
		this(uri, HttpMethod.GET);
	}

	public MockHttpServerRequest(String uri, HttpMethod m, UnicastSubject<ByteBuf> content) {
		this(null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, m, uri), content);
	}

	public MockHttpServerRequest(String uri, HttpMethod m) {
		this(null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, m, uri), noContent());
	}

	protected static UnicastSubject<ByteBuf> noContent() {
		return toContent(new byte[0]);
	}

	public MockHttpServerRequest(String url, HttpMethod m, String body) {
		this(null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, m, url), toContent(body));

	}

	public MockHttpServerRequest(String url, HttpMethod m, byte[] body) {
		this(null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, m, url), toContent(body));

	}

	private static UnicastSubject<ByteBuf> toContent(byte[] body) {
		UnicastSubject<ByteBuf> content = UnicastSubject.create();
		ByteBuf buf = Unpooled.wrappedBuffer(body == null ? new byte[0] : body);
		content.onNext(buf);
		content.onCompleted();
		return content;
	}

	private static UnicastSubject<ByteBuf> toContent(String body) {
		if (body == null) {
			UnicastSubject<ByteBuf> content = UnicastSubject.create();
			content.onCompleted();
			return content;
		}
		return toContent(body.getBytes());
	}

	public void addHeader(String key, String val) {
		req.headers().add(key, val);
	}

	public void addCookie(String key, String val) {
		addHeader("Cookie", key + "=" + val);
	}
}
