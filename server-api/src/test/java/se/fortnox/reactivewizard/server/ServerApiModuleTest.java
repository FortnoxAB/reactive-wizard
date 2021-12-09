package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import se.fortnox.reactivewizard.binding.scanners.InjectAnnotatedScanner;
import se.fortnox.reactivewizard.config.ConfigFactory;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceInterceptor;

import java.text.DateFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ServerApiModuleTest {

    @Test
    public void shouldBindServerStuff() {
        InjectAnnotatedScanner injectAnnotatedScanner = new InjectAnnotatedScanner();

        ConfigFactory mockedConfigFactory = Mockito.mock(ConfigFactory.class);
        when(mockedConfigFactory.get(ServerConfig.class))
            .thenReturn(new ServerConfig());
        Injector injector = Guice.createInjector(new ServerApiModule(injectAnnotatedScanner, mockedConfigFactory),
            new AbstractModule() {
                @Override
                protected void configure() {
                    Multibinder.newSetBinder(binder(), JaxRsResourceInterceptor.class)
                        .addBinding().toProvider(() -> new JaxRsResourceInterceptor() {
                            @Override
                            public void preHandle(JaxRsResourceContext context) {
                                JaxRsResourceInterceptor.super.preHandle(context);
                            }

                            @Override
                            public void postHandle(JaxRsResourceContext context, Publisher<Void> resourceCall) {
                                JaxRsResourceInterceptor.super.postHandle(context, resourceCall);
                            }
                        });
                }
            });

        assertThat(injector.getInstance(DateFormat.class) instanceof StdDateFormat)
            .isTrue();
    }
}
