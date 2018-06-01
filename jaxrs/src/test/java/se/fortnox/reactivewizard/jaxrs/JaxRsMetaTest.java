package se.fortnox.reactivewizard.jaxrs;

import org.junit.Test;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;

public class JaxRsMetaTest {

    @Test
    public void shouldFindJaxRsClassFromImplementation() {
        Optional<Class<?>> jaxRsClass = JaxRsMeta.getJaxRsClass(ResourceImplementingInterface.class);
        assertThat(jaxRsClass.isPresent()).isTrue();
        assertThat(jaxRsClass.get()).isEqualTo(ResourceInterface.class);
    }

    @Test
    public void shouldFindJaxRsClassFromImplementationWithoutInterface() {
        Optional<Class<?>> jaxRsClass = JaxRsMeta.getJaxRsClass(ResourceImplementation.class);
        assertThat(jaxRsClass.isPresent()).isTrue();
        assertThat(jaxRsClass.get()).isEqualTo(ResourceImplementation.class);
    }

    @Test
    public void shouldNotReturnJaxRsClassFromInterface() {
        Optional<Class<?>> jaxRsClass = JaxRsMeta.getJaxRsClass(ResourceInterface.class);
        assertThat(jaxRsClass.isPresent()).isFalse();
    }

    @Path("/test")
    interface ResourceInterface {
        @GET
        Observable<String> hello();
    }

    static class ResourceImplementingInterface implements ResourceInterface {

        @Override
        public Observable<String> hello() {
            return null;
        }
    }

    @Path("/test")
    static class ResourceImplementation {
        @GET
        public Observable<String> hello(){
            return null;
        }
    }

}
