package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.binding.scanners.InjectAnnotatedScanner;
import se.fortnox.reactivewizard.config.ConfigFactory;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceInterceptor;

import java.text.DateFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ServerModuleTest {

    @Test
    public void shouldBindServerStuff() {
        Module bootstrap = new AbstractModule() {
            @Override
            protected void configure() {
                bind(String[].class).annotatedWith(Names.named("args")).toInstance(new String[]{});
                bind(ConfigFactory.class).toInstance(new ConfigFactory((String)null));
            }
        };

        Injector injector = Guice.createInjector(new AutoBindModules(bootstrap));

        assertThat(injector.getInstance(RwServer.class))
            .isNotNull();
    }
}
