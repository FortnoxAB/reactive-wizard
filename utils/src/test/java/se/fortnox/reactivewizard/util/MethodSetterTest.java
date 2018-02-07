package se.fortnox.reactivewizard.util;

import org.junit.Test;
import rx.Observable;

import static org.fest.assertions.Assertions.assertThat;

public class MethodSetterTest {
    @Test
    public void shouldSetValue() throws Exception {
        Setter setter = ReflectionUtil.getSetter(Foo.class, "value");
        Foo    foo    = new Foo(1);
        setter.invoke(foo, 9);
        assertThat(foo.field).isEqualTo(9);
    }


    @Test
    public void shouldGetReturnType() {
        Setter setter = ReflectionUtil.getSetter(Foo.class, "value");
        assertThat(setter.getParameterType()).isEqualTo(Integer.class);
    }

    @Test
    public void shouldGetGenericReturnType() {
        Setter setter = ReflectionUtil.getSetter(Foo.class, "observable");

        assertThat(setter.getGenericParameterType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
    }

    private class Foo {
        private int              field;
        private Observable<Long> longObservable;

        public Foo(int field) {
            this.field = field;
        }

        public void setValue(Integer value) {
            this.field = value;
        }


        public void setObservable(Observable<Long> longObservable) {
            this.longObservable = longObservable;
        }
    }

}
