package se.fortnox.reactivewizard.jaxrs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Optional;
import java.util.stream.Stream;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class JaxRsMetaTest {

    @ParameterizedTest
    @MethodSource("classesAndInterfaces")
    void shouldFindJaxRsClass(Class<?> implementation, Class<?> jaxRsClassOrInterface) {
        Optional<Class<?>> jaxRsClass = JaxRsMeta.getJaxRsClass(implementation);
        assertThat(jaxRsClass.isPresent()).isTrue();
        assertThat(jaxRsClass.get()).isEqualTo(jaxRsClassOrInterface);
    }

    @ParameterizedTest
    @MethodSource("resourceMethods")
    void producesAnnotation(String methodName, boolean isAnnotationPresent, String annotationValue) throws NoSuchMethodException {
        JaxRsMeta jaxRsMeta = new JaxRsMeta(ResourceInterface.class.getMethod(methodName));
        assertThat(jaxRsMeta.isProducesAnnotationPresent()).isEqualTo(isAnnotationPresent);
        Assertions.assertThat(jaxRsMeta.getProduces()).isEqualTo(annotationValue);
    }

    @Test
    void shouldNotReturnJaxRsClassFromInterface() {
        Optional<Class<?>> jaxRsClass = JaxRsMeta.getJaxRsClass(ResourceInterface.class);
        assertThat(jaxRsClass.isPresent()).isFalse();
    }

    private static Stream<Arguments> classesAndInterfaces() {
        return Stream.of(
            arguments(ResourceImplementingInterface.class, ResourceInterface.class),
            arguments(ResourceImplementation.class, ResourceImplementation.class)
        );
    }

    private static Stream<Arguments> resourceMethods() {
        return Stream.of(
            arguments("hello", false, "application/json"),
            arguments("someHtml", true, "text/html"),
            arguments("someJson", true, "application/json"),
            arguments("defaultContent", true, "*/*")
        );
    }

    @Path("/test")
    interface ResourceInterface {
        @GET
        Observable<String> hello();

        @GET
        @Produces(TEXT_HTML)
        Flux<String> someHtml();

        @GET
        @Produces(APPLICATION_JSON)
        Flux<String> someJson();

        @GET
        @Produces
        Flux<String> defaultContent();
    }

    static class ResourceImplementingInterface implements ResourceInterface {

        @Override
        public Observable<String> hello() {
            return null;
        }

        @Override
        public Flux<String> someHtml() {
            return Flux.empty();
        }

        @Override
        public Flux<String> someJson() {
            return Flux.empty();
        }

        @Override
        public Flux<String> defaultContent() {
            return Flux.empty();
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
