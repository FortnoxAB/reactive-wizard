package se.fortnox.reactivewizard;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import rx.subjects.UnicastSubject;

/**
 * Created by jonashall on 2015-11-26.
 */
public class MockContent {
    public static UnicastSubject<ByteBuf> noContent() {
        return toContent(new byte[0]);
    }

    public static UnicastSubject<ByteBuf> toContent(String body) {
        return toContent(body == null ? new byte[0] : body.getBytes());
    }

    public static UnicastSubject<ByteBuf> toContent(byte[] body) {
        if (body == null) {
            body = new byte[0];
        }
        UnicastSubject<ByteBuf> content = UnicastSubject.create();
        ByteBuf                 buf     = Unpooled.wrappedBuffer(body);
        content.onNext(buf);
        content.onCompleted();
        return content;
    }
}
