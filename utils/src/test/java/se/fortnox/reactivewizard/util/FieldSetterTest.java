package se.fortnox.reactivewizard.util;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class FieldSetterTest {
    @Test
    public void shouldSetFinalField() throws Exception {
        Setter setter = ReflectionUtil.getSetter(Foo.class, "value");
        Foo    foo    = new Foo(1);
        setter.invoke(foo, 9);
        assertThat(foo.value).isEqualTo(9);
    }

    private class Foo {
        private final int value;

        private Foo(int value) {
            this.value = value;
        }
    }
}
