package se.fortnox.reactivewizard.validation;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import se.fortnox.reactivewizard.binding.scanners.ConfigClassScanner;
import se.fortnox.reactivewizard.binding.scanners.InjectAnnotatedScanner;
import se.fortnox.reactivewizard.config.ConfigAutoBindModule;

import static org.mockito.Mockito.mock;

public class ValidationModuleTest {

    @Test
    public void validationModuleShouldGoAfterConfigModule() {
        InjectAnnotatedScanner injectAnnotatedScanner = mock(InjectAnnotatedScanner.class);
        ValidationModule validationModule = new ValidationModule(injectAnnotatedScanner);

        ConfigClassScanner configClassScanner = mock(ConfigClassScanner.class);
        ConfigAutoBindModule configAutoBindModule = new ConfigAutoBindModule(configClassScanner);

        Assertions.assertThat(configAutoBindModule.getPrio()).isLessThan(validationModule.getPrio());
    }
}
