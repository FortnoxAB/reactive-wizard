package se.fortnox.reactivewizard.springserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

@Singleton
public class TestCache {
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Inject
    public TestCache(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }
}
