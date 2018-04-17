package se.fortnox.reactivewizard.db.deserializing;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class MutabilityDetectorTest {
    @Test
    public void testIsImmutable() {
        assertThat(MutabilityDetector.isImmutable(Immutable.class)).isTrue();
        assertThat(MutabilityDetector.isImmutable(Mutable.class)).isFalse();
    }

    @Test
    public void shouldIgnoreSyntheticMembers() throws Exception {
        Class<?> withSynthetic = createClassWithSyntheticMembers();
        assertThat(MutabilityDetector.isImmutable(withSynthetic)).isTrue();
    }

    private Class<?> createClassWithSyntheticMembers() throws NoSuchMethodException {
        return new ByteBuddy().subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
            .defineField("synthetic", TypeDescription.STRING, SyntheticState.SYNTHETIC, Visibility.PRIVATE)
            .defineField("notSynthetic", TypeDescription.STRING, Visibility.PRIVATE, FieldManifestation.FINAL)
            .defineMethod("getSynthetic", TypeDescription.STRING, Visibility.PUBLIC)
            .intercept(FieldAccessor.ofField("synthetic"))
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(TypeDescription.STRING)
            .intercept(MethodCall.invoke(Object.class.getDeclaredConstructor()).onSuper())
            .defineConstructor(Visibility.PUBLIC, SyntheticState.SYNTHETIC)
            .intercept(MethodCall.invoke(Object.class.getDeclaredConstructor()).onSuper())
            .make()
            .load(getClass().getClassLoader())
            .getLoaded();
    }

    private static class Immutable {
        private final String test;

        private Immutable(String test) {
            this.test = test;
        }

        public String getTest() {
            return test;
        }
    }

    private static class Mutable {
        private String test;

        private Mutable(String test) {
            this.test = test;
        }

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }
    }
}
