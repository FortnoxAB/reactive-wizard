package se.fortnox.reactivewizard.reactorclient;

public class RwHttpClientResponse {
    private reactor.netty.http.client.HttpClientResponse httpClientResponse;
    private String                                 content;

    public RwHttpClientResponse(reactor.netty.http.client.HttpClientResponse httpClientResponse, String content) {
        this.httpClientResponse = httpClientResponse;
        this.content = content;
    }

    public reactor.netty.http.client.HttpClientResponse getHttpClientResponse() {
        return httpClientResponse;
    }

    public String getContent() {
        return content;
    }
}
