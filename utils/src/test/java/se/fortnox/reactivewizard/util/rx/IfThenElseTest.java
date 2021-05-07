package se.fortnox.reactivewizard.util.rx;

import org.junit.Test;
import rx.Observable;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class IfThenElseTest {

    @Test
    @SuppressWarnings("unchecked")
    public void shouldExecuteThen() {
        Consumer<Boolean> thenMock = mock(Consumer.class);
        Observable<Boolean> ret = new IfThenElse<Boolean>(just(true))
            .then(just(true).doOnNext(thenMock::accept))
            .elseThrow(new Throwable());

        ret.toBlocking().single();

        verify(thenMock, times(1)).accept(anyBoolean());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldThrow() {
        Consumer<Boolean> thenMock = mock(Consumer.class);
        Observable<Boolean> ret = new IfThenElse<Boolean>(just(false))
            .then(just(true).doOnNext(thenMock::accept))
            .elseThrow(new Throwable());

        try {
            ret.toBlocking().single();
            fail("Expected exception, but none was thrown");
        } catch (Throwable e) {
        }

        verify(thenMock, never()).accept(anyBoolean());
    }
}
