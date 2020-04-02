package se.fortnox.reactivewizard.reactorclient;

import io.netty.handler.codec.http.HttpMethod;
import se.fortnox.reactivewizard.client.RequestBuilder;

import java.net.InetSocketAddress;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ReactorRequestBuilder extends RequestBuilder {
    private String content = "";

    public ReactorRequestBuilder(InetSocketAddress serverInfo, HttpMethod method, String key) {
        super(serverInfo, method, key);
    }

    public String getContent() {
        return content;
    }

    @Override
    public void setContent(String content) {
        super.setContent(content);
    }

    @Override
    public void setContent(byte[] content) {
        super.setContent(content);
        this.content = new String(content, UTF_8);
    }
}
