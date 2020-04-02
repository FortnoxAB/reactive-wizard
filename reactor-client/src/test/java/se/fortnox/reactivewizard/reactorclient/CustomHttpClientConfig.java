package se.fortnox.reactivewizard.reactorclient;

import se.fortnox.reactivewizard.client.HttpClientConfig;
import se.fortnox.reactivewizard.client.UseInResource;
import se.fortnox.reactivewizard.config.Config;

@Config(value = "customHttpClient")
@UseInResource(RestClientFactoryTest.CustomTestResource.class)
public class CustomHttpClientConfig extends HttpClientConfig {
}
