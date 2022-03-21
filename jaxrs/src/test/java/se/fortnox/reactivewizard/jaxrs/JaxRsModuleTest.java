package se.fortnox.reactivewizard.jaxrs;

import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class JaxRsModuleTest {
    @Test
    public void shouldRegisterInterceptorBinding() {
        TypeLiteral<Set<JaxRsResourceInterceptor>> type = new TypeLiteral<>() {};
        Set<JaxRsResourceInterceptor> interceptors = Guice.createInjector(new JaxRsModule()).getInstance(Key.get(type));

        assertThat(interceptors).isNotNull();
    }
}
