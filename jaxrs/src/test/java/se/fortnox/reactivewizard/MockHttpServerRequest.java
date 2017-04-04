package se.fortnox.reactivewizard;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;

import java.util.List;
import java.util.Map;

import static se.fortnox.reactivewizard.MockContent.noContent;
import static se.fortnox.reactivewizard.MockContent.toContent;

public class MockHttpServerRequest extends HttpServerRequest<ByteBuf> {

	private HttpRequest					req;
	private Map<String, List<String>>	query;

	protected MockHttpServerRequest(Channel channel, HttpRequest nettyRequest,
			UnicastContentSubject<ByteBuf> content) {
		super(channel, nettyRequest, content);
		this.req = nettyRequest;
	}

	public MockHttpServerRequest(String uri) {
		this(null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri), noContent());
	}

	public MockHttpServerRequest(String uri, UnicastContentSubject<ByteBuf> content) {
		this(null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri), content);
	}

	public MockHttpServerRequest(String uri, HttpMethod m) {
		this(null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, m, uri), noContent());
	}


	public MockHttpServerRequest(String uri, Map<String, List<String>> query) {
		this(uri);
		this.query = query;
	}

	public MockHttpServerRequest(String url, HttpMethod m, String body) {
		this(null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, m, url), toContent(body));
	}

	public MockHttpServerRequest(String url, HttpMethod m, byte[] body) {
		this(null, new DefaultHttpRequest(HttpVersion.HTTP_1_1, m, url), toContent(body));
	}

	@Override
	public Map<String, List<String>> getQueryParameters() {
		return query != null ? query : super.getQueryParameters();
	}

	public void addHeader(String key, String val) {
		req.headers().add(key, val);
	}

	public void addCookie(String key, String val) {
		addHeader("Cookie", key + "=" + val);
	}

}
