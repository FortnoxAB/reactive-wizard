package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import guice.module.SpringModule;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.binding.scanners.InjectAnnotatedScanner;
import se.fortnox.reactivewizard.config.ConfigFactory;
import se.fortnox.reactivewizard.jaxrs.ByteBufCollector;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsResources;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourcesProvider;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactory;
import se.fortnox.reactivewizard.jaxrs.response.NoContentTransformer;
import se.fortnox.reactivewizard.jaxrs.response.ResponseDecoratorTransformer;
import se.fortnox.reactivewizard.jaxrs.response.ResultTransformerFactory;
import se.fortnox.reactivewizard.logging.LoggingFactory;

import java.nio.file.NoSuchFileException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
@Configuration
public class RwServer3 {

    private static List<Class<?>> listOfStuff = new ArrayList<>();

    public static void main(String[] args) throws NoSuchMethodException {




      //  new AutoBindModules(bootstrap)
     /*   Injector injector = Guice.createInjector(new SpringModule(context), binder -> {
            System.out.println("im here!");


            Multibinder<RequestHandler> requestHandlers = Multibinder.newSetBinder(binder,
                new TypeLiteral<>() { });

            Multibinder.newSetBinder(binder, TypeLiteral.get(ParamResolverFactory.class));
            Multibinder<ResultTransformerFactory> resultTransformers = Multibinder.newSetBinder(binder,
                TypeLiteral.get(ResultTransformerFactory.class));
            resultTransformers.addBinding().to(NoContentTransformer.class);
            resultTransformers.addBinding().to(ResponseDecoratorTransformer.class);
            Multibinder.newSetBinder(binder, TypeLiteral.get(ParamResolver.class));
            binder.bind(DateFormat.class).toProvider(StdDateFormat::new);

            ByteBufCollector byteBufCollector = new ByteBufCollector(10000);
            binder.bind(ByteBufCollector.class).toInstance(byteBufCollector);

            JaxRsResourceRegistry jaxRsResourceRegistry = new JaxRsResourceRegistry();
            binder.bind(JaxRsResourceRegistry.class).toInstance(jaxRsResourceRegistry);
            binder.bind(JaxRsResourcesProvider.class).toInstance(jaxRsResourceRegistry);


        });

     //   JaxRsResourceRegistry        registry = injector.getInstance(JaxRsResourceRegistry.class);

        RequestMappingHandlerMapping requestMappingHandlerMapping    = injector.getInstance(RequestMappingHandlerMapping.class);

        TestResource testResource = new TestResource();
        requestMappingHandlerMapping.registerMapping(RequestMappingInfo.paths("api").methods(RequestMethod.GET).build(), testResource, TestResource.class.getMethod("doStuff"));
*/
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
            ConfigurableApplicationContext context = SpringApplication.run(RwServer2.class, args);

            Injector injector = Guice.createInjector(new SpringModule(context), new AutoBindModules(bootstrap));
            JaxRsResourceRegistry        registry = injector.getInstance(JaxRsResourceRegistry.class);
          //  injector.getInstance(JaxRsResources.class).findResource()

            RequestMappingHandlerMapping requestMappingHandlerMapping    = injector.getInstance(RequestMappingHandlerMapping.class);

            for(Object resource : registry.getResources()) {
          //      resource.getClass()

                requestMappingHandlerMapping.registerMapping(RequestMappingInfo.paths("api").methods(RequestMethod.GET).build(), resource, TestResource.class.getMethod("doStuff"));

            }
            System.out.println("helllo");
        } catch (Exception e) {
            // Since logging is configured at runtime we cant have a static logger.
            LoggerFactory
                .getLogger(RwServer3.class)
                .error("Caught exception at startup.", e);
            System.exit(-1);
        }

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

    /*


    @Autowired
    public void setHandlerMapping(RequestMappingHandlerMapping mapping, UserHandler handler)
        throws NoSuchMethodException {

        RequestMappingInfo info = RequestMappingInfo
            .paths("/user/{id}").methods(RequestMethod.GET).build();

        Method method = UserHandler.class.getMethod("getUser", Long.class);

        mapping.registerMapping(info, handler, method);
    }

     */
}
