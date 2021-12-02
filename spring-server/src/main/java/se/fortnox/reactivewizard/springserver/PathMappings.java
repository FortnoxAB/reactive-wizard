package se.fortnox.reactivewizard.springserver;

import io.netty.handler.codec.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.server.JaxRsResourceRegistry;

import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;

import static com.google.common.base.MoreObjects.firstNonNull;

@Component
public class PathMappings {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    public PathMappings(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        handleMappings();
    }

    /**
     * Accept all incoming requests
     */
    public void handleMappings() {
        final RequestMappingInfo.BuilderConfiguration builderConfiguration = new RequestMappingInfo.BuilderConfiguration();

        builderConfiguration.setContentTypeResolver(new FixedContentTypeResolver(org.springframework.http.MediaType.APPLICATION_JSON));
        RequestMappingInfo info = RequestMappingInfo
            .paths("/**")
            .produces(MediaType.APPLICATION_JSON)
            .options(builderConfiguration)
            .build();

        DummyHandler dummyHandler = new DummyHandler();
        try {
            requestMappingHandlerMapping.registerMapping(info, dummyHandler, DummyHandler.class.getMethod("toString"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private class DummyHandler {

    }
}
