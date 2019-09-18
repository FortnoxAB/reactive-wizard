import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.slf4j.MDC;
import se.fortnox.reactivewizard.logging.LoggingContextProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingContextProviderTest {

    private static final LoggingContextProvider PROVIDER = new LoggingContextProvider();

    private static final Map<String, String> STATE_1 = ImmutableMap.of("someKey", "somveValue", "someOtherKey", "somveOtherValue");
    private static final Map<String, String> STATE_2 = ImmutableMap.of("eeny", "meeny", "miny", "moe");

    @Test
    public void shouldInstallContextWhenNonNull() {
        assertInstall(STATE_1, STATE_2, STATE_2);
    }

    @Test
    public void shouldNotInstallContextWhenNull() {
        assertInstall(STATE_1, null, STATE_1);
    }

    @Test
    public void shouldRestoreContextWhenNonNull() {
        assertRestore(STATE_1, STATE_1);
    }

    @Test
    public void shouldNotRestoreContextWhenNull() {
        assertRestore(null, null);
    }

    @Test
    public void shouldCaptureCurrentContext() {
        MDC.setContextMap(STATE_1);

        assertThat(PROVIDER.capture())
            .isEqualTo(STATE_1);
    }

    private static void assertInstall(Map<String, String> previousState, Map<String, String> stateToBeInstalled, Map<String, String> expectedState) {

        MDC.setContextMap(previousState);

        Map<String, String> actualPreviousState = PROVIDER.install(stateToBeInstalled);

        Map<String, String> actualState = MDC.getCopyOfContextMap();
        assertThat(actualPreviousState)
            .isEqualTo(previousState);
        assertThat(actualState)
            .isEqualTo(expectedState);
    }

    private static void assertRestore(Map<String, String> stateToBeRestored, Map<String, String> expectedState) {

        PROVIDER.restore(stateToBeRestored);

        assertThat(MDC.getCopyOfContextMap())
            .isEqualTo(expectedState);
    }
}
