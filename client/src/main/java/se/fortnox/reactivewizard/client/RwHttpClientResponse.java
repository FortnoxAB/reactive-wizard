package se.fortnox.reactivewizard.client;

import reactor.netty.ByteBufFlux;

public class RwHttpClientResponse {
    private reactor.netty.http.client.HttpClientResponse httpClientResponse;
    private ByteBufFlux                                  byteBufFlux;

    public RwHttpClientResponse(reactor.netty.http.client.HttpClientResponse httpClientResponse, ByteBufFlux byteBufFlux) {
        this.httpClientResponse = httpClientResponse;
        this.byteBufFlux = byteBufFlux;
    }

    public reactor.netty.http.client.HttpClientResponse getHttpClientResponse() {
        return httpClientResponse;
    }

    public ByteBufFlux getContent() {
        return byteBufFlux;
    }
}
