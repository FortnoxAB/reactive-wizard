package se.fortnox.reactivewizard.jaxrs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private static final boolean PRODUCES_ANNOTATION_PRESENT = true;
    private static final boolean PRODUCES_ANNOTATION_NOT_PRESENT = false;

    @ParameterizedTest
    @MethodSource("classesAndInterfaces")
    void shouldFindJaxRsClass(Class<?> implementation, Class<?> jaxRsClassOrInterface) {
        Optional<Class<?>> jaxRsClass = JaxRsMeta.getJaxRsClass(implementation);
        assertThat(jaxRsClass)
            .contains(jaxRsClassOrInterface);
    }

    private static Stream<Arguments> classesAndInterfaces() {
        return Stream.of(
            arguments(ResourceImplementingInterface.class, ResourceInterface.class),
            arguments(ResourceImplementation.class, ResourceImplementation.class)
        );
    }

    @ParameterizedTest
    @MethodSource("resourceMethods")
    void shouldSetProducesAnnotation(String methodName, boolean isAnnotationPresent, String annotationValue) throws NoSuchMethodException {
        // On the interface of the resource
        JaxRsMeta jaxRsMeta = new JaxRsMeta(ResourceInterface.class.getMethod(methodName));
        assertThat(jaxRsMeta.isProducesAnnotationPresent()).isEqualTo(isAnnotationPresent);
        assertThat(jaxRsMeta.getProduces()).isEqualTo(annotationValue);

        // On the implementation of a resource without interface
        jaxRsMeta = new JaxRsMeta(ResourceImplementation.class.getMethod(methodName));
        assertThat(jaxRsMeta.isProducesAnnotationPresent()).isEqualTo(isAnnotationPresent);
        assertThat(jaxRsMeta.getProduces()).isEqualTo(annotationValue);
    }

    private static Stream<Arguments> resourceMethods() {
        return Stream.of(
            arguments("hello", PRODUCES_ANNOTATION_NOT_PRESENT, "application/json"),
            arguments("someHtml", PRODUCES_ANNOTATION_PRESENT, "text/html"),
            arguments("someJson", PRODUCES_ANNOTATION_PRESENT, "application/json"),
            arguments("defaultContent", PRODUCES_ANNOTATION_PRESENT, "*/*")
        );
    }

    @Test
    void shouldNotReturnJaxRsClassFromInterface() {
        Optional<Class<?>> jaxRsClass = JaxRsMeta.getJaxRsClass(ResourceInterface.class);
        assertThat(jaxRsClass).isNotPresent();
    }

    @ParameterizedTest
    @MethodSource("pathChecks")
    void shouldSetFullPath(Class<?> resource, String methodName, String expectedPath) throws NoSuchMethodException {
        JaxRsMeta jaxRsMeta = new JaxRsMeta(resource.getMethod(methodName));
        assertThat(jaxRsMeta.getFullPath()).isEqualTo(expectedPath);
    }

    private static Stream<Arguments> pathChecks() {
        return Stream.of(
            arguments(ResourceInterfaceWithoutMainPath.class, "methodWithNoLeadingSlash", "/noLeadingSlash"),
            arguments(ResourceInterfaceWithoutMainPath.class, "methodWithLeadingSlash", "/leadingSlash"),
            arguments(ResourceInterface.class, "hello", "/test/hello")
        );
    }

    @Test
    void shouldGetDeprecationFromMethodOnInterface() throws NoSuchMethodException {
        JaxRsMeta jaxRsMeta = new JaxRsMeta(ResourceInterface.class.getMethod("deprecated"));
        assertThat(jaxRsMeta.isDeprecated()).isTrue();
    }

    @Test
    void shouldGetDeprecationFromMethodOnImplementation() throws NoSuchMethodException {
        JaxRsMeta jaxRsMeta = new JaxRsMeta(ResourceImplementation.class.getMethod("deprecated"));
        assertThat(jaxRsMeta.isDeprecated()).isTrue();
    }

    @Test
    void shouldGetDeprecationFromImplementingClass() throws NoSuchMethodException {
        JaxRsMeta jaxRsMeta = new JaxRsMeta(DeprecatedResource.class.getMethod("deprecated"));
        assertThat(jaxRsMeta.isDeprecated()).isTrue();
    }

    @Test
    void shouldNotSetDeprecatedUnlessAnnotated() throws NoSuchMethodException {
        JaxRsMeta jaxRsMeta = new JaxRsMeta(ResourceImplementation.class.getMethod("hello"));
        assertThat(jaxRsMeta.isDeprecated()).isFalse();

        jaxRsMeta = new JaxRsMeta(ResourceInterface.class.getMethod("hello"));
        assertThat(jaxRsMeta.isDeprecated()).isFalse();
    }

    @Path("/test")
    interface ResourceInterface {

        @Path("hello")
        @GET
        Mono<String> hello();

        @SuppressWarnings("DeprecatedIsStillUsed")
        @Path("deprecated")
        @GET
        @Deprecated
        Mono<String> deprecated();

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
        public Mono<String> hello() {
            return null;
        }

        @Override
        public Mono<String> deprecated() {
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
        public Mono<String> hello() {
            return null;
        }

        @SuppressWarnings("DeprecatedIsStillUsed")
        @GET
        @Deprecated
        public Mono<String> deprecated() {
            return null;
        }

        @GET
        @Produces(TEXT_HTML)
        public Flux<String> someHtml() {
            return null;
        }

        @GET
        @Produces(APPLICATION_JSON)
        public Flux<String> someJson() {
            return null;
        }

        @GET
        @Produces
        public Flux<String> defaultContent() {
            return Flux.empty();
        }
    }

    interface ResourceInterfaceWithoutMainPath {
        @Path("noLeadingSlash")
        @GET
        Mono<Void> methodWithNoLeadingSlash();

        @Path("/leadingSlash")
        @GET
        Mono<Void> methodWithLeadingSlash();
    }

    @Deprecated
    interface DeprecatedResource {
        @Path("deprecated")
        @GET
        Mono<String> deprecated();
    }

    interface DeprecatedImplementingResource {
        @Path("deprecated")
        @GET
        Mono<String> deprecated();
    }

    @Deprecated
    static class DeprecatedImplementingResourceImpl implements DeprecatedImplementingResource {
        @Override
        public Mono<String> deprecated() {
            return null;
        }
    }
}
