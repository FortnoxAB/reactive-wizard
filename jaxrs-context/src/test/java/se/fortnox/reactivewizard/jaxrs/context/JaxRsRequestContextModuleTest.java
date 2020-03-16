package se.fortnox.reactivewizard.jaxrs.context;

import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.fest.assertions.Condition;
import org.junit.Test;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceInterceptor;

import java.util.Collection;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class JaxRsRequestContextModuleTest {
    @Test
    public void shouldRegisterInterceptor() {
        TypeLiteral<Set<JaxRsResourceInterceptor>> type = new TypeLiteral<Set<JaxRsResourceInterceptor>>(){};
        Set<JaxRsResourceInterceptor> interceptors = Guice.createInjector(new JaxRsRequestContextModule())
            .getInstance(Key.get(type));

        assertThat(interceptors).isNotNull().satisfies(containsInterceptor());
    }

    private Condition<Collection<?>> containsInterceptor() {
        return new Condition<Collection<?>>() {
            @Override
            public boolean matches(Collection<?> value) {
                return value.stream().anyMatch(JaxRsRequestContextInterceptor.class::isInstance);
            }
        };
    }
}
