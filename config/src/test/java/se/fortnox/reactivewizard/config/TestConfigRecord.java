package se.fortnox.reactivewizard.config;

@Config("myTestConfig")
public record TestConfigRecord(
    String myKey,
    String configWithEnvPlaceholder,
    String configWithEnvPlaceholder2,
    String configWithEnvPlaceholderInMiddle,
    String url
) {
}
