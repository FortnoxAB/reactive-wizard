package se.fortnox.reactivewizard.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.ws.rs.PathParam;
import java.util.Collections;

import static reactor.core.publisher.Mono.just;

@Configuration
public class MvcConfigurer implements WebFluxConfigurer {

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(PathParam.class);
            }

            @Override
            public Mono<Object> resolveArgument(MethodParameter parameter,
                BindingContext bindingContext,
                ServerWebExchange exchange
            ) {
                String attributeName = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
                return just(exchange.getAttributeOrDefault(attributeName, Collections.emptyMap()).get(parameter.getParameterAnnotation(PathParam.class).value()));
            }
        });
    }
}
