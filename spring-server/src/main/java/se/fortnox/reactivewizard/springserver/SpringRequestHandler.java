package se.fortnox.reactivewizard.springserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.server.CompositeRequestHandler;

import java.util.List;

@Configuration
public class SpringRequestHandler implements WebFluxConfigurer {

    @Bean
    public RwResultHandler rwResultHandler(ServerCodecConfigurer serverCodecConfigurer, RequestedContentTypeResolver resolver, CompositeRequestHandler compositeRequestHandler) {
        return new RwResultHandler(serverCodecConfigurer.getWriters(), resolver, compositeRequestHandler);
    }

    private class RwResultHandler extends ResponseBodyResultHandler {
        private final CompositeRequestHandler compositeRequestHandler;

        public RwResultHandler(List<HttpMessageWriter<?>> writers,
                               RequestedContentTypeResolver resolver,
                               CompositeRequestHandler compositeRequestHandler
        ) {
            super(writers, resolver);
            this.compositeRequestHandler = compositeRequestHandler;
        }

        @Override
        public boolean supports(HandlerResult result) {
            return true;
        }

        @Override
        public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
            return Mono.from(compositeRequestHandler.apply(
                ((AbstractServerHttpRequest) exchange.getRequest()).getNativeRequest(),
                ((AbstractServerHttpResponse) exchange.getResponse()).getNativeResponse()));

        }
    }
}
