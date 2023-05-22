package se.fortnox.reactivewizard.config;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

class TestTestInjector {


    @Test
    void testCreateWithOutConfigFile() {
        Injector injector = TestInjector.create();
        assertThat(mockingDetails(injector.getInstance(ConfigFactory.class)).isMock())
            .isTrue();

        String[] args = injector.getInstance(Key.get(String[].class, Names.named("args")));
        assertThat(args).isEmpty();
    }

    @Test
    void testCreateWithConfigFile() {
        Injector injector = TestInjector.create("src/test/resources/testconfig.yml");
        assertThat(injector.getInstance(ConfigAutoBindModule.class).getClass().getName()).isEqualToIgnoringCase(ConfigAutoBindModule.class.getName());
        assertThat(mockingDetails(injector.getInstance(ConfigFactory.class)).isMock())
            .isFalse();

        String[] args = injector.getInstance(Key.get(String[].class, Names.named("args")));
        assertThat(args.length).isOne();
        assertThat(args[0]).isEqualTo("src/test/resources/testconfig.yml");
    }
}
