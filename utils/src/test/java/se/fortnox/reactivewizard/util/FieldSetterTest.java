package se.fortnox.reactivewizard.util;

import org.junit.Test;
import rx.Observable;

import static org.fest.assertions.Assertions.assertThat;
import static rx.Observable.empty;

public class FieldSetterTest {
    @Test
    public void shouldSetFinalField() throws Exception {
        Setter setter = ReflectionUtil.getSetter(Foo.class, "value");
        Foo    foo    = new Foo(1);
        setter.invoke(foo, 9);
        assertThat(foo.value).isEqualTo(9);
    }

    @Test
    public void shouldGetReturnType() {
        Setter setter = ReflectionUtil.getSetter(Foo.class, "value");
        assertThat(setter.getParameterType()).isEqualTo(Integer.class);
    }

    @Test
    public void shouldGetGenericReturnType() {
        Setter setter = ReflectionUtil.getSetter(Foo.class, "longObservable");
        assertThat(setter.getGenericParameterType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
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
