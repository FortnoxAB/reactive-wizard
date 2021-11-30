package se.fortnox.reactivewizard.springserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;

import javax.ws.rs.Consumes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

                var meta = new JaxRsMeta(parameter.getMethod());
                var jaxRsRequest = new JaxRsRequest(((AbstractServerHttpRequest)exchange.getRequest()).getNativeRequest());
                var pathPattern = createPathPattern(meta.getFullPath());

                jaxRsRequest.matchesPath(pathPattern);

                return resolver.resolve(jaxRsRequest);
            }


        };
    }

    private static Pattern createPathPattern(String path) {
        // Vars with custom regex, like this: {myvar:myregex}
        path = path.replaceAll("\\{([^}]+):([^}]+)\\}", "(?<$1>$2)");
        // Vars without custom regex, like this: {myvar}
        path = path.replaceAll("\\{([^}]+)\\}", "(?<$1>[^/]+)");
        // Allow trailing slash
        path = "^" + path + "[/\\s]*$";

        return Pattern.compile(path);
    }
}
