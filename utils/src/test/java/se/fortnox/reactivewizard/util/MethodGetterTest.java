package se.fortnox.reactivewizard.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class MethodGetterTest extends AccessorTest {
    private Getter value;
    private Getter longObservable;
    private Getter genericSuperKey;
    private Getter genericSuperValue;
    private Getter superKey;
    private Getter superValue;

    public MethodGetterTest(boolean useLambdas) {
        LambdaCompiler.useLambdas = useLambdas;
        value = ReflectionUtil.getGetter(GenericMethodSubclass.class, "value");
        longObservable = ReflectionUtil.getGetter(GenericMethodSubclass.class, "longObservable");
        genericSuperKey = ReflectionUtil.getGetter(GenericMethodSubclass.class, "superKey");
        genericSuperValue = ReflectionUtil.getGetter(GenericMethodSubclass.class, "superValue");
        superKey = ReflectionUtil.getGetter(MethodSubclass.class, "superKey");
        superValue = ReflectionUtil.getGetter(MethodSubclass.class, "superValue");
    }

    @Parameterized.Parameters
    public static Collection useLambdasParameters() {
        return List.of(new Object[][] {{ true }, { false }});
    }

    @Test
    public void shouldCreateMethodGetters() {
        assertThat(Stream.of(value, longObservable, genericSuperKey, genericSuperValue, superKey, superValue).allMatch(MethodGetter.class::isInstance)).isTrue();
    }

    @Test
    public void shouldGetValue() throws Exception {
        assertThat(value.invoke(new GenericMethodSubclass(5))).isEqualTo(5);
        assertThat(genericSuperKey.invoke(new GenericMethodSubclass(5))).isEqualTo("5");
        assertThat(genericSuperValue.invoke(new GenericMethodSubclass(5))).isEqualTo(5);
        assertThat(superKey.invoke(new MethodSubclass("1", 2))).isEqualTo("1");
        assertThat(superValue.invoke(new MethodSubclass("1", 2))).isEqualTo(2);
    }

    @Test
    public void shouldGetReturnType() {
        assertThat(value.getReturnType()).isEqualTo(Integer.class);
        assertThat(genericSuperKey.getReturnType()).isEqualTo(String.class);
        assertThat(genericSuperValue.getReturnType()).isEqualTo(Integer.class);
        assertThat(superKey.getReturnType()).isEqualTo(String.class);
        assertThat(superValue.getReturnType()).isEqualTo(Integer.class);
    }

    @Test
    public void shouldGetGenericReturnType() {
        assertThat(longObservable.getGenericReturnType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
        assertThat(genericSuperKey.getGenericReturnType().toString()).isEqualTo("class java.lang.String");
        assertThat(genericSuperValue.getGenericReturnType().toString()).isEqualTo("class java.lang.Integer");
        assertThat(superKey.getGenericReturnType().toString()).isEqualTo("class java.lang.String");
        assertThat(superValue.getGenericReturnType().toString()).isEqualTo("class java.lang.Integer");
    }


}
