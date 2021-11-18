package se.fortnox.reactivewizard.server;

import org.springframework.context.annotation.Bean;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

public class SpringRequestHandler {
    @Bean
    public ReactorHttpHandlerAdapter webHandler() {
        return new ReactorHttpHandlerAdapter(new HttpHandler() {
            @Override
            public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
                return Mono.empty();
            }
        });
    }
}
