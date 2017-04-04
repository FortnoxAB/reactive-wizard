package se.fortnox.reactivewizard;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.netty.protocol.http.UnicastContentSubject;

/**
 * Created by jonashall on 2015-11-26.
 */
public class MockContent {

	public static UnicastContentSubject<ByteBuf> noContent() {
		return toContent(new byte[0]);
	}

	public static UnicastContentSubject<ByteBuf> toContent(String body) {
		return toContent(body == null ? new byte[0] : body.getBytes());
	}

	public static UnicastContentSubject<ByteBuf> toContent(byte[] body) {
		if (body == null) {
			body = new byte[0];
		}
		UnicastContentSubject<ByteBuf> content = UnicastContentSubject
				.createWithoutNoSubscriptionTimeout();
		ByteBuf buf = Unpooled.wrappedBuffer(body);
		content.onNext(buf);
		content.onCompleted();
		return content;
	}
}
