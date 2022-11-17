package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import se.fortnox.reactivewizard.config.Config;

/**
 * Configures JSON behavior
 */
@Config(value = "json")
public class JsonConfig {
    /**
     * Note: @JsonUnwrapped annotation doesn't work during serialization when lambda serializer modifier is enabled
     */
    @JsonProperty("useLambdaSerializerModifier")
    private boolean useLambdaSerializerModifier = true;

    public boolean isUseLambdaSerializerModifier() {
        return useLambdaSerializerModifier;
    }

    public void setUseLambdaSerializerModifier(boolean useLambdaSerializerModifier) {
        this.useLambdaSerializerModifier = useLambdaSerializerModifier;
    }
}
