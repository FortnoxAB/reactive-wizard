package se.fortnox.reactivewizard.client;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestParameterSerializersTest {
    private RequestParameterSerializers serializers;

    @Before
    public void setUp() {
        serializers = new RequestParameterSerializers(Set.of(new Foo(), new Bar()));
    }

    @Test
    public void shouldProvideSerializerByClass() {
        assertThat(serializers.getSerializer(Foo.class)).isInstanceOf(Foo.class);
        assertThat(serializers.getSerializer(Bar.class)).isInstanceOf(Bar.class);
    }

    private class Foo implements RequestParameterSerializer<Foo> {
        @Override
        public void addParameter(Foo param, RequestBuilder request) {

        }
    }

    private class Bar implements RequestParameterSerializer<Bar> {
        @Override
        public void addParameter(Bar param, RequestBuilder request) {

        }
    }
}
