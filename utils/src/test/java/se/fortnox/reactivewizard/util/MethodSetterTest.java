package se.fortnox.reactivewizard.util;

import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import java.util.stream.Stream;

import static org.fest.assertions.Assertions.assertThat;

public class MethodSetterTest {
    private Setter value;
    private Setter longObservable;
    private Setter superKey;
    private Setter superValue;

    @Before
    public void setUp() {
        value = ReflectionUtil.getSetter(Foo.class, "value");
        longObservable = ReflectionUtil.getSetter(Foo.class, "longObservable");
        superKey = ReflectionUtil.getSetter(Foo.class, "superKey");
        superValue = ReflectionUtil.getSetter(Foo.class, "superValue");
    }

    @Test
    public void shouldCreateMethodSetters() {
        assertThat(Stream.of(value, longObservable, superKey, superValue).allMatch(MethodSetter.class::isInstance)).isTrue();
    }

    @Test
    public void shouldSetValue() throws Exception {
        Foo foo = new Foo(1);
        value.invoke(foo, 9);
        assertThat(foo.field).isEqualTo(9);

        superKey.invoke(foo, "9");
        assertThat(foo.getSuperKey()).isEqualTo("9");

        superValue.invoke(foo, 9);
        assertThat(foo.getSuperValue());
    }

    @Test
    public void shouldGetParameterType() {
        assertThat(value.getParameterType()).isEqualTo(Integer.class);
        assertThat(superKey.getParameterType()).isEqualTo(String.class);
        assertThat(superValue.getParameterType()).isEqualTo(Integer.class);
    }

    @Test
    public void shouldGetGenericParameterType() {
        assertThat(longObservable.getGenericParameterType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
        assertThat(superKey.getGenericParameterType().toString()).isEqualTo("class java.lang.String");
        assertThat(superValue.getGenericParameterType().toString()).isEqualTo("class java.lang.Integer");
    }

    private class Foo extends GenericSuper<String, Integer> {
        private int              field;
        private Observable<Long> longObservable;

        public Foo(int field) {
            this.field = field;
        }

        public void setValue(Integer value) {
            this.field = value;
        }

        public void setLongObservable(Observable<Long> longObservable) {
            this.longObservable = longObservable;
        }
    }

    private class GenericSuper<K, V> {
        private K superKey;
        private V superValue;

        K getSuperKey() {
            return superKey;
        }

        public void setSuperKey(K superKey) {
            this.superKey = superKey;
        }

        V getSuperValue() {
            return superValue;
        }

        public void setSuperValue(V superValue) {
            this.superValue = superValue;
        }
    }
}
