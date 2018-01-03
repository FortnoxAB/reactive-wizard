package se.fortnox.reactivewizard.config;

@Config("myTestConfig")
public class TestConfig {
    private String myKey;
    private String configWithEnvPlaceholder;
    private String configWithEnvPlaceholderInMiddle;
    private String url;

    public String getMyKey() {
        return myKey;
    }

    public void setMyKey(String myKey) {
        this.myKey = myKey;
    }

    public String getConfigWithEnvPlaceholder() {
        return configWithEnvPlaceholder;
    }

    public void setConfigWithEnvPlaceholder(String configWithEnvPlaceholder) {
        this.configWithEnvPlaceholder = configWithEnvPlaceholder;
    }

    public String getConfigWithEnvPlaceholderInMiddle() {
        return configWithEnvPlaceholderInMiddle;
    }

    public void setConfigWithEnvPlaceholderInMiddle(String configWithEnvPlaceholderInMiddle) {
        this.configWithEnvPlaceholderInMiddle = configWithEnvPlaceholderInMiddle;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
