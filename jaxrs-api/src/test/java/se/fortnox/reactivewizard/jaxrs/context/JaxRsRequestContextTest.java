package se.fortnox.reactivewizard.jaxrs.context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class JaxRsRequestContextTest {
    @Before
    public void setUp() {
        JaxRsRequestContext.open();
    }

    @After
    public void tearDown() {
        JaxRsRequestContext.close();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailOnReentry() {
        JaxRsRequestContext.open();
    }

    @Test
    public void shouldStoreValuesWhenInContext() {
        JaxRsRequestContext.setValue("foo", "bar");
        assertThat(JaxRsRequestContext.getValue("foo").orElse(null)).isEqualTo("bar");

        JaxRsRequestContext.close();
        assertThat(JaxRsRequestContext.getValue("foo").isEmpty()).isTrue();
    }
}
