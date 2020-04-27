package se.fortnox.reactivewizard.reactorclient;

import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.client.RequestBuilder;

import java.net.InetSocketAddress;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

public class ReactorRequestBuilder extends RequestBuilder {
    public ReactorRequestBuilder(InetSocketAddress serverInfo, HttpMethod method, String key) {
        super(serverInfo, method, key);
    }

    public Mono<RwHttpClientResponse> submit(
        reactor.netty.http.client.HttpClient client,
        ReactorRequestBuilder requestBuilder) {

        return
            Mono.from(client
                .headers(entries -> {
                    for (Map.Entry<String, String> stringStringEntry : requestBuilder.getHeaders().entrySet()) {
                        entries.set(stringStringEntry.getKey(), stringStringEntry.getValue());
                    }

                    if (requestBuilder.getContent() != null) {
                        entries.set(CONTENT_LENGTH, this.getContent().length);
                    }
                })
                .request(requestBuilder.getHttpMethod())
                .uri(requestBuilder.getFullUrl())
                .send((httpClientRequest, nettyOutbound)
                    -> nettyOutbound.sendByteArray(this.getContent() != null ? Mono.just(this.getContent()) : Mono.empty()))
                .responseConnection((httpClientResponse, connection)
                    -> Mono.just(new RwHttpClientResponse(httpClientResponse, connection.inbound().receive()))));
    }
}
