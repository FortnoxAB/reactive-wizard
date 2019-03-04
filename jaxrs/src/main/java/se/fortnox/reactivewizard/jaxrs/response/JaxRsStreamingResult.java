package se.fortnox.reactivewizard.jaxrs.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import rx.Observable;
import rx.functions.Func1;
import se.fortnox.reactivewizard.jaxrs.WebException;

import java.util.Map;

import static rx.Observable.empty;
import static rx.Observable.just;
import static se.fortnox.reactivewizard.util.rx.FirstThen.first;

public class JaxRsStreamingResult<T> extends JaxRsResult<T> {
    public JaxRsStreamingResult(Observable<T> output, HttpResponseStatus responseStatus, Func1<T, byte[]> serializer, Map<String, Object> headers) {
        super(output, responseStatus, serializer, headers);
    }

    @Override
    public Observable<Void> write(HttpServerResponse<ByteBuf> response) {

        return output
            .first()
            .onErrorResumeNext(throwable -> {

                if (throwable instanceof WebException) {
                    response.setStatus(((WebException)throwable).getStatus());
                } else {
                    response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                }

                ObjectMapper objectMapper = new ObjectMapper();

                headers.forEach(response::addHeader);

                byte[] bytes;
                try {
                    bytes = objectMapper.writeValueAsBytes(throwable);
                    return first(response.writeBytes(just(bytes))).thenReturn(empty());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return empty();
                }
            })
            .flatMap(t -> {
                response.setStatus(responseStatus);
                headers.forEach(response::addHeader);
                return response.writeBytesAndFlushOnEach(output.map(serializer));
            });
    }
}
