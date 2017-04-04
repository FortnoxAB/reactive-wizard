package se.fortnox.reactivewizard;


import se.fortnox.reactivewizard.util.rx.RxUtils;
import org.junit.Test;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;

public class RxUtilsTest {
    @Test
    public void testConsolidate() {
        Bar bar1;
        Bar bar2;
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
        List<Integer> input = new ArrayList<Integer>() {{
            add(0);
            add(4);
            add(3);
            add(1);
            add(2);
        }};

        List<Integer> expectedResult = new ArrayList<Integer>() {{
            add(0);
            add(1);
            add(2);
            add(3);
            add(4);
        }};

        List<Integer> actualResult = RxUtils.async(input).flatMap(i -> Observable.just(i).delay(i * 100, TimeUnit.MILLISECONDS)).toList().toBlocking().single();

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test(expected = RuntimeException.class)
    public void testException() {
        Observable.empty()
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
