package se.fortnox.reactivewizard;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.mockito.Mockito.verify;

public class SetupModuleTest {

    @Test
    public void testLogging() {
        final Logger mock = Mockito.mock(Logger.class);
        final RuntimeException error = new RuntimeException("");

        final SetupModule setupModule = new SetupModule();
        setupModule.configure(null);
        setupModule.logError(mock, error);

        verify(mock).warn("Tried to send item or error to subscriber but the subscriber had already left. " +
            "This could happen when you merge two (or more) observables and one reports an error while the other a moment later tries to " +
            "call onError on the subscriber but the subscriber already left at the first error.", error);
    }


    /**
     *  Testing main to increase coverage
     */
    @Test
    public void testMain() {
        Main.main(new String[]{"dev.example.yml"});
        Main.main(new String[]{});
    }
}
