package se.fortnox.reactivewizard.util;

import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import java.util.stream.Stream;

import static org.fest.assertions.Assertions.assertThat;
import static rx.Observable.empty;

public class MethodGetterTest {
    private Getter value;
    private Getter longObservable;
    private Getter superKey;
    private Getter superValue;

    @Before
    public void setUp() {
        value = ReflectionUtil.getGetter(Foo.class, "value");
        longObservable = ReflectionUtil.getGetter(Foo.class, "longObservable");
        superKey = ReflectionUtil.getGetter(Foo.class, "superKey");
        superValue = ReflectionUtil.getGetter(Foo.class, "superValue");
    }

    @Test
    public void shouldCreateMethodGetters() {
        assertThat(Stream.of(value, longObservable, superKey, superValue).allMatch(MethodGetter.class::isInstance)).isTrue();
    }

    @Test
    public void shouldGetValue() throws Exception {
        assertThat(value.invoke(new Foo(5))).isEqualTo(5);
        assertThat(superKey.invoke(new Foo(5))).isEqualTo("5");
        assertThat(superValue.invoke(new Foo(5))).isEqualTo(5);
    }

    @Test
    public void shouldGetReturnType() {
        assertThat(value.getReturnType()).isEqualTo(Integer.class);
        assertThat(superKey.getReturnType()).isEqualTo(String.class);
        assertThat(superValue.getReturnType()).isEqualTo(Integer.class);
    }

    @Test
    public void shouldGetGenericReturnType() {
        assertThat(longObservable.getGenericReturnType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
        assertThat(superKey.getGenericReturnType().toString()).isEqualTo("class java.lang.String");
        assertThat(superValue.getGenericReturnType().toString()).isEqualTo("class java.lang.Integer");
    }

    private class Foo extends GenericSuper<String, Integer> {
        private final Integer          field;
        private final Observable<Long> longObservable;

        private Foo(Integer value) {
            super(String.valueOf(value), value);
            this.field = value;
            this.longObservable = empty();
        }

        public Integer getValue() {
            return field;
        }

        public Observable<Long> getLongObservable() {
            return longObservable;
        }
    }

    private class GenericSuper<K, V> {
        private final K superKey;
        private final V superValue;

        private GenericSuper(K key, V value) {
            this.superKey = key;
            this.superValue = value;
        }

        public K getSuperKey() {
            return superKey;
        }

        public V getSuperValue() {
            return superValue;
        }
    }
}
