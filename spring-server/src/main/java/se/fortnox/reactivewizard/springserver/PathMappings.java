package se.fortnox.reactivewizard.springserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

import javax.ws.rs.core.MediaType;

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
