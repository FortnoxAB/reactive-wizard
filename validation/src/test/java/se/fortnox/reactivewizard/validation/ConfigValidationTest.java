package se.fortnox.reactivewizard.validation;

import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import org.junit.Test;
import se.fortnox.reactivewizard.config.Config;
import se.fortnox.reactivewizard.config.TestInjector;

import javax.validation.constraints.NotEmpty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ConfigValidationTest {

    @Test
    public void shouldReturnErrorForInvalidConfig() {
        Injector injector = TestInjector.create("src/test/resources/invalidconfig.yml");
        try {
            injector.getInstance(ValidatedConfig.class);
            fail("expected exception");
        } catch (ProvisionException e) {
            assertThat(e.getCause()).isInstanceOf(ValidationFailedException.class);
            assertThat(((ValidationFailedException)e.getCause()).getFields()[0].getField()).isEqualTo("mykey");
        }
    }

    @Test
    public void shouldReturnErrorForInvalidConfigRecord() {
        Injector injector = TestInjector.create("src/test/resources/invalidconfig.yml");
        try {
            injector.getInstance(ValidatedConfigRecord.class);
            fail("expected exception");
        } catch (ProvisionException e) {
            assertThat(e.getCause()).isInstanceOf(ValidationFailedException.class);
            assertThat(((ValidationFailedException)e.getCause()).getFields()[0].getField()).isEqualTo("mykey");
        }
    }

    @Test
    public void shouldReturnConfigForCorrectConfig() {
        Injector injector = TestInjector.create("src/test/resources/correctconfig.yml");
        injector.getInstance(ValidatedConfig.class);
    }

    @Test
    public void shouldReturnConfigForCorrectConfigRecord() {
        Injector injector = TestInjector.create("src/test/resources/correctconfig.yml");
        var config = injector.getInstance(ValidatedConfigRecord.class);

        assertThat(config.mykey()).isEqualTo("myvalue");
        assertThat(config.anInt()).isEqualTo(0);
        assertThat(config.aNestedConfig()).isNull();
    }

    @Config("myconfig")
    public static class ValidatedConfig {
        @NotEmpty
        private String mykey;

        public String getMykey() {
            return mykey;
        }

        public void setMykey(String mykey) {
            this.mykey = mykey;
        }
    }

    @Config("myconfig")
    public record ValidatedConfigRecord(
        @NotEmpty String mykey,
        int anInt,
        ValidatedConfig aNestedConfig
    ) {
    }
}
