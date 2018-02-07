package se.fortnox.reactivewizard.util;

import org.junit.Test;
import rx.Observable;

import static org.fest.assertions.Assertions.assertThat;
import static rx.Observable.empty;

public class FieldGetterTest {
    @Test
    public void shouldGetValue() throws Exception {
        Getter getter = ReflectionUtil.getGetter(Foo.class, "value");
        assertThat(getter.invoke(new Foo(5))).isEqualTo(5);
    }

    @Test
    public void shouldGetReturnType() {
        Getter getter = ReflectionUtil.getGetter(Foo.class, "value");
        assertThat(getter.getReturnType()).isEqualTo(Integer.class);
    }

    @Test
    public void shouldGetGenericReturnType() {
        Getter getter = ReflectionUtil.getGetter(Foo.class, "longObservable");

        assertThat(getter.getGenericReturnType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
    }

    private class Foo {
        private final Integer          value;
        private final Observable<Long> longObservable;

        private Foo(Integer value) {
            this.value = value;
            longObservable = empty();
        }

    }

}
