package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import rx.Observable;
import rx.functions.Func1;

import java.util.Map;

public class JaxRsStreamingResult<T> extends JaxRsResult<T> {
    public JaxRsStreamingResult(Observable<T> output, HttpResponseStatus responseStatus, Func1<T, byte[]> serializer, Map<String,Object> headers) {
        super(output, responseStatus, serializer, headers);
    }

    @Override
    public Observable<Void> write(HttpServerResponse<ByteBuf> response) {
        response.setStatus(responseStatus);
        headers.forEach(response::addHeader);
        return response.writeBytesAndFlushOnEach(output.map(serializer));
    }
}
