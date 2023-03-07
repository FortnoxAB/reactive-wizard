package org.mockito.configuration;

import org.mockito.Answers;
import org.mockito.stubbing.Answer;

/**
 * Custom Mockito configuration that sets the default answer to use smart-nulls to make finding test errors easier.
 * See {@link org.mockito.Mockito#RETURNS_SMART_NULLS}.
 */
public class MockitoConfiguration implements IMockitoConfiguration {
    @Override
    public Answer<Object> getDefaultAnswer() {
        return Answers.RETURNS_SMART_NULLS;
    }

    @Override
    public boolean cleansStackTrace() {
        return true;
    }

    @Override
    public boolean enableClassCache() {
        return true;
    }
}
