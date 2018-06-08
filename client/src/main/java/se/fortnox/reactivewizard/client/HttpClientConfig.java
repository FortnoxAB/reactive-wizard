package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.fortnox.reactivewizard.config.Config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;

@Config("httpClient")
public class HttpClientConfig {

    private static final String CURLY_PLACEHOLDER_BEGIN = "begin-curly--";

    private static final String CURLY_PLACEHOLDER_END = "--end-curly";

    @Valid
    @JsonProperty("port")
    private int port = 80;

    @Valid
    @NotNull
    @JsonProperty("host")
    private String host;

    @Valid
    @JsonProperty("maxConnections")
    private int maxConnections = 1000;

    private String            url;
    private InetSocketAddress devServerInfo;
    @Size(min = 1)
    private String            devCookie;

    private Map<String, String> devHeaders;

    private int maxResponseSize = 10 * 1024 * 1024;

    private long    poolAutoCleanupInterval = TimeUnit.MILLISECONDS.convert(10, MINUTES);
    private long    maxRequestTime          = TimeUnit.MILLISECONDS.convert(1, MINUTES);
    private boolean isHttps;
    private int     retryCount              = 3;
    private int     retryDelayMs            = 1000;

    public HttpClientConfig() {
    }

    public HttpClientConfig(String url) throws URISyntaxException {
        setUrl(url);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) throws URISyntaxException {
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        // Replace all curly braces with a placeholder string, because they are
        // not allowed in host names. We want to allow them in order to replace
        // part of the host name with a variable.
        String validUrl = url.replaceAll("\\{", CURLY_PLACEHOLDER_BEGIN)
            .replaceAll("\\}", CURLY_PLACEHOLDER_END);
        URI uri = new URI(validUrl);
        host = uri.getHost();
        if (host == null) {
            throw new RuntimeException("Could not parse host from " + url);
        } else {
            host = host.replaceAll(CURLY_PLACEHOLDER_BEGIN, "{")
                .replaceAll(CURLY_PLACEHOLDER_END, "}");
        }

        isHttps = "https".equals(uri.getScheme());
        port = uri.getPort();
        if (port < 0) {
            if (isHttps) {
                port = 443;
            } else {
                port = 80;
            }
        }
    }

    public InetSocketAddress getDevServerInfo() {
        return devServerInfo;
    }

    public void setDevServerInfo(InetSocketAddress devServerInfo) {
        this.devServerInfo = devServerInfo;
    }

    public String getDevCookie() {
        return devCookie;
    }

    public void setDevCookie(String devCookie) {
        this.devCookie = devCookie;
    }

    public int getMaxResponseSize() {
        return maxResponseSize;
    }

    public void setMaxResponseSize(int maxResponseSize) {
        this.maxResponseSize = maxResponseSize;
    }

    public Map<String, String> getDevHeaders() {
        return devHeaders;
    }

    public void setDevHeaders(Map<String, String> devHeaders) {
        this.devHeaders = devHeaders;
    }

    public boolean isHttps() {
        return isHttps;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(int retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
}
