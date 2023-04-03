package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.InjectAnnotatedScanner;
import se.fortnox.reactivewizard.config.ConfigFactory;
import se.fortnox.reactivewizard.jaxrs.ByteBufCollector;
import se.fortnox.reactivewizard.jaxrs.JaxRsMeta;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourcesProvider;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactory;
import se.fortnox.reactivewizard.jaxrs.response.NoContentTransformer;
import se.fortnox.reactivewizard.jaxrs.response.ResponseDecoratorTransformer;
import se.fortnox.reactivewizard.jaxrs.response.ResultTransformerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.DateFormat;
import java.util.Optional;

/**
 * Makes all necessary bindings for setting up an @{@link RwServer} with @{@link JaxRsRequestHandler}.
 */
@Singleton
public class ServerModule implements AutoBindModule {

    private final InjectAnnotatedScanner injectAnnotatedScanner;
    private final ServerConfig            config;
    private final ServerConfigurerScanner serverConfigurerScanner;

    @Inject
    public ServerModule(InjectAnnotatedScanner injectAnnotatedScanner, ConfigFactory configFactory, ServerConfigurerScanner serverConfigurerScanner) {
        this.injectAnnotatedScanner = injectAnnotatedScanner;
        this.config                 = configFactory.get(ServerConfig.class);
        this.serverConfigurerScanner = serverConfigurerScanner;
    }

    @Override
    public void configure(Binder binder) {
        Multibinder<RequestHandler> requestHandlers = Multibinder.newSetBinder(
            binder,
            new TypeLiteral<RequestHandler>() { });
        requestHandlers.addBinding().to(JaxRsRequestHandler.class);

        Multibinder.newSetBinder(binder, TypeLiteral.get(ParamResolverFactory.class));

        Multibinder<ResultTransformerFactory> resultTransformers = Multibinder.newSetBinder(binder,
                TypeLiteral.get(ResultTransformerFactory.class));

        resultTransformers.addBinding().to(NoContentTransformer.class);
        resultTransformers.addBinding().to(ResponseDecoratorTransformer.class);
        Multibinder.newSetBinder(binder, TypeLiteral.get(ParamResolver.class));
        binder.bind(DateFormat.class).toProvider(StdDateFormat::new);

        ByteBufCollector byteBufCollector = new ByteBufCollector(config.getMaxRequestSize());
        binder.bind(ByteBufCollector.class).toInstance(byteBufCollector);

        JaxRsResourceRegistry jaxRsResourceRegistry = new JaxRsResourceRegistry();
        binder.bind(JaxRsResourceRegistry.class).toInstance(jaxRsResourceRegistry);
        binder.bind(JaxRsResourcesProvider.class).toInstance(jaxRsResourceRegistry);


        for (Class<?> cls : injectAnnotatedScanner.getClasses()) {
            Optional<Class<?>> jaxRsClass = JaxRsMeta.getJaxRsClass(cls);
            if (jaxRsClass.isPresent()) {
                jaxRsResourceRegistry.add(binder.getProvider(jaxRsClass.get()));
            }
        }

        binder.bind(RwServer.class).asEagerSingleton();

        Multibinder<ReactorServerConfigurer> serverModifierMultibinder = Multibinder.newSetBinder(binder, new TypeLiteral<ReactorServerConfigurer>() {
        });
        serverConfigurerScanner.getClasses().forEach(serverModifierClass ->
            serverModifierMultibinder.addBinding().to((Class<? extends ReactorServerConfigurer>)serverModifierClass));
    }
}
