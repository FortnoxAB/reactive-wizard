package se.fortnox.reactivewizard.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FieldGetterTest extends AccessorTest {
    private Getter<GenericFieldSubclass, Integer> value;
    private Getter longObservable;
    private Getter genericSuperKey;
    private Getter genericSuperValue;
    private Getter superKey;
    private Getter superValue;

    @BeforeEach
    public void setUp() {
        value = ReflectionUtil.getGetter(GenericFieldSubclass.class, "value");
        longObservable = ReflectionUtil.getGetter(GenericFieldSubclass.class, "longObservable");
        genericSuperKey = ReflectionUtil.getGetter(GenericFieldSubclass.class, "superKey");
        genericSuperValue = ReflectionUtil.getGetter(GenericFieldSubclass.class, "superValue");
        superKey = ReflectionUtil.getGetter(FieldSubclass.class, "superKey");
        superValue = ReflectionUtil.getGetter(FieldSubclass.class, "superValue");
    }

    @Test
    void shouldCreateFieldGetters() {
        assertThat(Stream.of(value, longObservable, genericSuperKey, genericSuperValue, superKey, superValue).allMatch(FieldGetter.class::isInstance)).isTrue();
    }

    @Test
    void shouldGetValue() throws Exception {
        assertThat(value.invoke(new GenericFieldSubclass(5))).isEqualTo(5);
        assertThat(genericSuperKey.invoke(new GenericFieldSubclass(5))).isEqualTo("5");
        assertThat(genericSuperValue.invoke(new GenericFieldSubclass(5))).isEqualTo(5);
        assertThat(superKey.invoke(new FieldSubclass(5))).isEqualTo("5");
        assertThat(superValue.invoke(new FieldSubclass(5))).isEqualTo(5);
    }

    @Test
    void shouldSupportLambdaGetter() {
        Function<GenericFieldSubclass, Integer> getter = value.getterFunction();
        assertThat(getter.apply(new GenericFieldSubclass(5))).isEqualTo(5);

    }

    @Test
    void shouldGetReturnType() {
        assertThat(value.getReturnType()).isEqualTo(Integer.class);
        assertThat(genericSuperKey.getReturnType()).isEqualTo(String.class);
        assertThat(genericSuperValue.getReturnType()).isEqualTo(Integer.class);
        assertThat(superKey.getReturnType()).isEqualTo(String.class);
        assertThat(superValue.getReturnType()).isEqualTo(Integer.class);
    }

    @Test
    void shouldGetGenericReturnType() {
        assertThat(longObservable.getGenericReturnType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
        assertThat(genericSuperKey.getGenericReturnType().toString()).isEqualTo("class java.lang.String");
        assertThat(genericSuperValue.getGenericReturnType().toString()).isEqualTo("class java.lang.Integer");
        assertThat(superKey.getGenericReturnType().toString()).isEqualTo("class java.lang.String");
        assertThat(superValue.getGenericReturnType().toString()).isEqualTo("class java.lang.Integer");
    }
}
