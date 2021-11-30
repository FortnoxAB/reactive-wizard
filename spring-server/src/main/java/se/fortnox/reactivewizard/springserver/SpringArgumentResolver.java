package se.fortnox.reactivewizard.springserver;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.multipart.HttpData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServerFormDecoderProvider;
import reactor.netty.http.server.HttpServerRequest;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;

import javax.ws.rs.Consumes;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
public class SpringArgumentResolver {

    private final ParamResolverFactories paramResolverFactories;

    @Autowired
    public SpringArgumentResolver(ParamResolverFactories paramResolverFactories) {
        this.paramResolverFactories = paramResolverFactories;
    }

    @Bean
    public HandlerMethodArgumentResolver configureArgumentResolver() {
        return new HandlerMethodArgumentResolver() {

            Map<MethodParameter, ParamResolver> methodParamResolvers = new ConcurrentHashMap<>();

            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return true;
            }

            @Override
            public Mono<Object> resolveArgument(@NonNull MethodParameter parameter,
                @NonNull BindingContext bindingContext,
                @NonNull ServerWebExchange exchange
            ) {

                ParamResolver resolver = methodParamResolvers.computeIfAbsent(parameter, methodParameter -> {
                    return paramResolverFactories.createParamResolver(parameter.getMethod(),
                        parameter.getMethod().isAnnotationPresent(Consumes.class) ?
                        parameter.getMethod().getAnnotation(Consumes.class).value() : new String[]{}, parameter.getParameter());
                });

                return resolver.resolve(new JaxRsRequest(((AbstractServerHttpRequest)exchange.getRequest()).getNativeRequest()));

                //String attributeName = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
                //return just(
                //    exchange.getAttributeOrDefault(attributeName, Collections.emptyMap()).get(parameter.getParameterAnnotation(PathParam.class).value()));
            }
        };
    }
}
