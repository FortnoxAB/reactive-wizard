package se.fortnox.reactivewizard.validation;

import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import jakarta.validation.constraints.NotEmpty;
import org.junit.jupiter.api.Test;
import se.fortnox.reactivewizard.config.Config;
import se.fortnox.reactivewizard.config.TestInjector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ConfigValidationTest {

    @Test
    void shouldReturnErrorForInvalidConfig() {
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
    void shouldReturnErrorForInvalidConfigRecord() {
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
    void shouldReturnConfigForCorrectConfig() {
        Injector injector = TestInjector.create("src/test/resources/correctconfig.yml");
        injector.getInstance(ValidatedConfig.class);
    }

    @Test
    void shouldReturnConfigForCorrectConfigRecord() {
        Injector injector = TestInjector.create("src/test/resources/correctconfig.yml");
        var config = injector.getInstance(ValidatedConfigRecord.class);

        assertThat(config.mykey()).isEqualTo("myvalue");
        assertThat(config.anInt()).isZero();
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
