package se.fortnox.reactivewizard.server;

import io.netty.handler.codec.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;

import static com.google.common.base.MoreObjects.firstNonNull;

@Singleton
public class MappingSetup {

    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    private JaxRsResourceRegistry jaxRsResourceRegistry;

    @Inject
    public MappingSetup(RequestMappingHandlerMapping requestMappingHandlerMapping, JaxRsResourceRegistry jaxRsResourceRegistry) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.jaxRsResourceRegistry = jaxRsResourceRegistry;
        handleMappings();
    }

    public void handleMappings() {
        for (Object resource : jaxRsResourceRegistry.getResources()) {
            for (Method declaredMethod : resource.getClass().getDeclaredMethods()) {
                JaxRsMeta jaxRsMeta = new JaxRsMeta(declaredMethod);
                if (jaxRsMeta.getHttpMethod() == null) {
                    continue;
                }
                final RequestMappingInfo.BuilderConfiguration builderConfiguration = new RequestMappingInfo.BuilderConfiguration();

                builderConfiguration.setContentTypeResolver(new FixedContentTypeResolver(org.springframework.http.MediaType.APPLICATION_JSON));

                RequestMappingInfo info = RequestMappingInfo
                    .paths(jaxRsMeta.getFullPath())
                    .methods(toRequestMethod(jaxRsMeta.getHttpMethod()))
                    .produces(firstNonNull(jaxRsMeta.getProduces(), MediaType.APPLICATION_JSON))
                    .options(builderConfiguration)
                    .build();

                requestMappingHandlerMapping.registerMapping(info, resource, declaredMethod);
            }
        }
    }

    private static RequestMethod toRequestMethod(HttpMethod httpMethod) {
        return switch (httpMethod.toString()) {
            case "GET" -> RequestMethod.GET;
            case "POST" -> RequestMethod.POST;
            case "PUT" -> RequestMethod.PUT;
            case "PATCH" -> RequestMethod.PATCH;
            case "DELETE" -> RequestMethod.DELETE;

            default -> RequestMethod.GET;
        };
    }

}
