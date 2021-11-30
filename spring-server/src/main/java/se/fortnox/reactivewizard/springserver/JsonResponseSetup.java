package se.fortnox.reactivewizard.springserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.ws.rs.PathParam;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import static reactor.core.publisher.Mono.just;

@Configuration
public class JsonResponseSetup implements WebFluxConfigurer {

    @Bean
    public TestResultHandler tstResultHAndler(ServerCodecConfigurer serverCodecConfigurer, RequestedContentTypeResolver resolver) {
        return new TestResultHandler(serverCodecConfigurer.getWriters(), resolver);
    }

    private class TestResultHandler extends ResponseBodyResultHandler {
        public TestResultHandler(List<HttpMessageWriter<?>> writers,
                                 RequestedContentTypeResolver resolver
        ) {
            super(writers, resolver);
        }

        @Override
        public boolean supports(HandlerResult result) {
            return true;
        }

        @Override
        public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
            Object body = result.getReturnValue();
            MethodParameter bodyTypeParameter = result.getReturnTypeSource();

            final Type type = ReflectionUtil.getTypeOfObservable(((InvocableHandlerMethod) result.getHandler()).getMethod());

            if (type.getTypeName().equals("java.lang.String")) {
                body = Flux.from((Mono) body).map(t -> {
                    return "\"" + t + "\"";
                });
            }

            return writeBody(body, bodyTypeParameter, exchange);
        }
    }
}
