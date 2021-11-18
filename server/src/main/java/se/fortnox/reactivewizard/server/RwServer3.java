package se.fortnox.reactivewizard.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.guice.module.SpringModule;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.result.method.annotation.PathVariableMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.config.ConfigFactory;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.logging.LoggingFactory;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.nio.file.NoSuchFileException;

import static com.google.common.base.MoreObjects.firstNonNull;

public class RwServer3 {

    public static void main(String[] args) {
        try {
            ConfigFactory  configFactory  = createConfigFactory(args);
            LoggingFactory loggingFactory = configFactory.get(LoggingFactory.class);
            loggingFactory.init();

            Module bootstrap = new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String[].class).annotatedWith(Names.named("args")).toInstance(args);
                    bind(ConfigFactory.class).toInstance(configFactory);
                }
            };


            final ConfigurableApplicationContext configurableApplicationContext = SpringApplication.run(RwServerConfig.class, args);
            final Injector injector = Guice.createInjector(new AutoBindModules(bootstrap), new SpringModule(configurableApplicationContext));

            RequestMappingHandlerMapping           requestMappingHandlerMapping = injector.getInstance(RequestMappingHandlerMapping.class);

            JaxRsResourceRegistry jaxRsResourceRegistry = injector.getInstance(JaxRsResourceRegistry.class);
            for (Object resource : jaxRsResourceRegistry.getResources()) {
                for (Method declaredMethod : resource.getClass().getDeclaredMethods()) {
                    JaxRsMeta jaxRsMeta = new JaxRsMeta(declaredMethod);

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

        } catch (Exception e) {
            // Since logging is configured at runtime we cant have a static logger.
            LoggerFactory
                .getLogger(RwServer3.class)
                .error("Caught exception at startup.", e);
            System.exit(-1);
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

    /**
     * Prepares a ConfigFactory before setting up Guice.
     * <p>
     * As logging can be part of the configuration file and the configuration file
     * could be missing, we have a side effect of setting up and initializing a LoggingFactory
     * that doesn't depend on ConfigFactory, if the configuration file is missing.
     *
     * @param args commandline arguments
     * @return A ConfigFactory based on a configuration file.
     * @throws NoSuchFileException If the configuration file cannot be found.
     */
    private static ConfigFactory createConfigFactory(String[] args) throws NoSuchFileException {
        try {
            return new ConfigFactory(args);
        } catch (RuntimeException runtimeException) {
            Throwable cause = runtimeException.getCause();
            if (cause != null && cause.getClass().isAssignableFrom(NoSuchFileException.class)) {
                LoggingFactory loggingFactory = new LoggingFactory();
                loggingFactory.init();
                throw (NoSuchFileException)cause;
            }
            throw runtimeException;
        }
    }
}
