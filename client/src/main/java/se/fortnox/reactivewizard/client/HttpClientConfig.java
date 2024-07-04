package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.fortnox.reactivewizard.config.Config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;

@Config("httpClient")
public class HttpClientConfig {

    public static final int DEFAULT_TIMEOUT_MS = 10_000;

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
    private String root;
    private InetSocketAddress devServerInfo;
    @Size(min = 1)
    private String            devCookie;

    private Map<String, String> devHeaders;

    private int maxResponseSize = 10 * 1024 * 1024;

    private boolean isHttps;
    private int     retryCount             = 3;
    private int     retryDelayMs           = 1000;
    private int     timeoutMs              = DEFAULT_TIMEOUT_MS;
    private int     readTimeoutMs          = 10000;
    private int     poolAcquireTimeoutMs   = 10000;
    @JsonProperty("validateCertificates")
    private boolean isValidateCertificates = true;
    private boolean followRedirect         = false;

    private long connectionMaxIdleTimeInMs         = TimeUnit.MILLISECONDS.convert(10, MINUTES);
    private int  numberOfConnectionFailuresAllowed = 10;

    private BasicAuthConfig basicAuth;

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
        this.url = url;
        if (!url.contains("://")) {
            this.url = "http://" + url;
        }
        URI uri = new URI(this.url);
        setHost(uri.getHost());
        setRoot(uri.getPath());

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

    public void setRoot(String root) {
        this.root = root;
    }

    public String getRoot() {
        return root;
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

    public void setHost(String host) {
        this.host = host;
        try {
            InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Cannot resolve host for httpClient: " + host, e);
        }
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isValidateCertificates() {
        return isHttps() && isValidateCertificates;
    }

    public void setValidateCertificates(boolean value) {
        isValidateCertificates = value;
    }

    public boolean isFollowRedirect() {
        return followRedirect;
    }

    public void setFollowRedirect(boolean value) {
        followRedirect = value;
    }

    public BasicAuthConfig getBasicAuth() {
        return basicAuth;
    }

    public void setBasicAuth(BasicAuthConfig basicAuth) {
        this.basicAuth = basicAuth;
    }

    public void setBasicAuth(String username, String password) {
        this.basicAuth = new BasicAuthConfig()
            .setUsername(username)
            .setPassword(password);
    }

    public int getPoolAcquireTimeoutMs() {
        return poolAcquireTimeoutMs;
    }

    public void setPoolAcquireTimeoutMs(int poolAcquireTimeoutMs) {
        this.poolAcquireTimeoutMs = poolAcquireTimeoutMs;
    }

    public long getConnectionMaxIdleTimeInMs() {
        return connectionMaxIdleTimeInMs;
    }

    public void setConnectionMaxIdleTimeInMs(long connectionMaxIdleTimeInMs) {
        this.connectionMaxIdleTimeInMs = connectionMaxIdleTimeInMs;
    }

    public int getNumberOfConnectionFailuresAllowed() {
        return numberOfConnectionFailuresAllowed;
    }

    public void setNumberOfConnectionFailuresAllowed(int numberOfConnectionFailuresAllowed) {
        this.numberOfConnectionFailuresAllowed = numberOfConnectionFailuresAllowed;
    }
}
