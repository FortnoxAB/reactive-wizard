package se.fortnox.reactivewizard.jaxrs.context;

import io.reactiverse.reactivecontexts.core.Context;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.fest.assertions.Assertions.assertThat;

public class JaxRsRequestContextPropagationTest {
    @Before
    public void setUp() {
        Context.load();
    }

    @Test
    public void shouldPropagateContextThroughObservables() {
        JaxRsRequestContext.open();
        Observable<String> delayedCall = Observable.just("foo")
            .delay(10, MILLISECONDS)
            .map(s -> String.format("%s %s", s, JaxRsRequestContext.getValue("foo").orElse(null)));
        JaxRsRequestContext.setValue("foo", "bar");
        JaxRsRequestContext.close();

        assertThat(delayedCall.toBlocking().single()).isEqualTo("foo bar");
    }
}
