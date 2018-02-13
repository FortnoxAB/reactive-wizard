package se.fortnox.reactivewizard.util.rx;

import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static rx.Observable.empty;
import static rx.Observable.error;
import static rx.Observable.just;

public class RxUtilsTest {
    @Test
    public void testFirst() {
        assertThat(RxUtils.first(just(true))).isInstanceOf(FirstThen.class);
    }

    @Test
    public void testIfTrue() {
        assertThat(RxUtils.ifTrue(just(true))).isInstanceOf(IfThenElse.class);
    }

    @Test
    public void testSum() {
        Double sum = RxUtils.sum(just(3d, 3d, 3d))
            .toBlocking()
            .single();
        assertThat(sum).isEqualTo(9d);
    }

    @Test
    public void shouldNotDoIfObservableIsEmpty() {
        Observable<Boolean> observable = just(true, false, true);

        Consumer<Boolean> thenMock = mock(Consumer.class);
        RxUtils.doIfEmpty(observable, () -> thenMock.accept(true)).toBlocking().subscribe();

        verify(thenMock, never()).accept(anyBoolean());
    }

    @Test
    public void shouldDoIfObservableIsEmpty() {
        Observable<Object> observable   = empty();

        Consumer<Boolean> thenMock = mock(Consumer.class);
        RxUtils.doIfEmpty(observable, () -> thenMock.accept(true)).toBlocking().subscribe();

        verify(thenMock).accept(eq(true));
    }


    @Test
    public void shouldNotDoIfObservableFails() {
        Observable<Object> observable = error(new RuntimeException());

        Consumer<Boolean> thenMock = mock(Consumer.class);
        try {
            RxUtils.doIfEmpty(observable, () -> thenMock.accept(true)).toBlocking().subscribe();
        } catch (RuntimeException e) {

        }

        verify(thenMock, never()).accept(anyBoolean());
    }

    @Test
    public void testConsolidate() {
        Bar             bar1;
        Bar             bar2;
        Observable<Foo> fooObservable = Observable.just(new Foo(), new Foo());
        Observable<Bar> barObservable = Observable.just(bar1 = new Bar(), bar2 = new Bar());

        Observable<Foo> fooObservableResult = RxUtils.consolidate(fooObservable, barObservable.toList(), Foo::setBars);

        fooObservableResult.forEach(foo -> {
            assertThat(foo.getBars().size()).isEqualTo(2);
            assertThat(foo.getBars().get(0)).isEqualTo(bar1);
            assertThat(foo.getBars().get(1)).isEqualTo(bar2);
        });
    }

    @Test
    public void testAsync() {
        List<Integer> input          = Arrays.asList(0, 4, 3, 1, 2);
        List<Integer> expectedResult = Arrays.asList(0, 1, 2, 3, 4);

        List<Integer> actualResult = RxUtils.async(input)
            .flatMap(i -> Observable.just(i).delay(i * 100, TimeUnit.MILLISECONDS))
            .toList()
            .toBlocking()
            .single();

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test(expected = RuntimeException.class)
    public void testException() {
        empty()
            .switchIfEmpty(RxUtils.exception(RuntimeException::new))
            .toBlocking()
            .first();
    }

    class Foo {
        private List<Bar> bars;

        public List<Bar> getBars() {
            return bars;
        }

        public void setBars(List<Bar> bars) {
            this.bars = bars;
        }
    }

    class Bar {

    }
}
