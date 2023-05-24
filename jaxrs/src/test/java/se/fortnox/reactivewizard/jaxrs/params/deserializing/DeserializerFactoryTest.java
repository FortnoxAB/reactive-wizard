package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.fortnox.reactivewizard.json.JsonDeserializerFactory;

import java.text.DateFormat;

import static org.assertj.core.api.Assertions.assertThat;

class DeserializerFactoryTest {
    private static final Deserializer<Foo> FOO_DESERIALIZER = new FooDeserializer();
    private static final Deserializer<Boolean> BOOLEAN_DESERIALIZER = new BooleanDeserializer();

    private DeserializerFactory deserializerFactory;

    @BeforeEach
    public void setUp() {
        deserializerFactory = Guice.createInjector(new TestModule()).getInstance(DeserializerFactory.class);
    }

    @Test
    void shouldUseCustomDeserializer() {
        assertThat(deserializerFactory.getClassDeserializer(Foo.class)).isSameAs(FOO_DESERIALIZER);
    }

    @Test
    void shouldUseOverriddenCustomDeserializer() {
        assertThat(deserializerFactory.getClassDeserializer(Boolean.class)).isSameAs(BOOLEAN_DESERIALIZER);
    }

    private static class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(DateFormat.class).toInstance(new StdDateFormat());
            bind(JsonDeserializerFactory.class).toInstance(new JsonDeserializerFactory());
            var multibinder = Multibinder.newSetBinder(binder(), Deserializer.class);
            multibinder.addBinding().toInstance(FOO_DESERIALIZER);
            multibinder.addBinding().toInstance(BOOLEAN_DESERIALIZER);
        }
    }

    private record Foo(String value) {}
    private static class FooDeserializer implements Deserializer<Foo> {
        @Override
        public Foo deserialize(String value) throws DeserializerException {
            return new Foo(value);
        }
    }
}
