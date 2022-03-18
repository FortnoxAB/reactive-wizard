package se.fortnox.reactivewizard.jaxrs.context;

import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.Test;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceInterceptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class JaxRsRequestContextModuleTest {
    @Test
    public void shouldRegisterInterceptor() {
        TypeLiteral<Set<JaxRsResourceInterceptor>> type = new TypeLiteral<>(){};
        Set<JaxRsResourceInterceptor> interceptors = Guice.createInjector(new JaxRsRequestContextModule())
            .getInstance(Key.get(type));

        assertThat(interceptors)
                .anySatisfy(interceptor -> assertThat(interceptor).isInstanceOf(JaxRsRequestContextInterceptor.class));
    }
}
