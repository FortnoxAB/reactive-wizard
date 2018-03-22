package se.fortnox.reactivewizard.config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockConfigFactory {

    private MockConfigFactory() {
    }

    public static ConfigFactory create() {
        ConfigFactory configFactory = mock(ConfigFactory.class);
        when(configFactory.get(any())).thenAnswer(call -> call.getArgumentAt(0, Class.class).newInstance());
        return configFactory;
    }
}

