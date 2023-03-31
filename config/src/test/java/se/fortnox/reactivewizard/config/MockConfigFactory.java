package se.fortnox.reactivewizard.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.quality.Strictness.LENIENT;

public class MockConfigFactory {

    private MockConfigFactory() {
    }

    public static ConfigFactory create() {
        ConfigFactory configFactory = mock(ConfigFactory.class, withSettings().strictness(LENIENT));
        when(configFactory.get(any())).thenAnswer(call -> call.getArgument(0, Class.class).newInstance());
        return configFactory;
    }
}

