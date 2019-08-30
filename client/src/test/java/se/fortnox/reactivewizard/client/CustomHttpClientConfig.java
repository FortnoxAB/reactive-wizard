package se.fortnox.reactivewizard.client;

import se.fortnox.reactivewizard.config.Config;

@Config(value = "customHttpClient")
@UseInResource(RestClientFactoryTest.CustomTestResource.class)
public class CustomHttpClientConfig extends HttpClientConfig {
}
