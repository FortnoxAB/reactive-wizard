package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.schedulers.Schedulers;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.InjectAnnotatedScanner;
import se.fortnox.reactivewizard.jaxrs.BlockingResourceScheduler;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsServiceProvider;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactory;
import se.fortnox.reactivewizard.jaxrs.response.ResultTransformerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;
import java.util.concurrent.Executors;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

public class ServerModule implements AutoBindModule {

    private final ServerConfig serverConfig;
    private final InjectAnnotatedScanner injectAnnotatedScanner;

    @Inject
    public ServerModule(ServerConfig serverConfig, InjectAnnotatedScanner injectAnnotatedScanner) {
        this.serverConfig = serverConfig;
        this.injectAnnotatedScanner = injectAnnotatedScanner;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ReactiveWizardServer.class).asEagerSingleton();

        binder.bind(DateFormat.class).to(SimpleDateFormat.class);
        binder.bind(ObjectMapper.class).toProvider(()->
                new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            ).in(Scopes.SINGLETON);

        Multibinder.newSetBinder(binder, TypeLiteral.get(ParamResolverFactory.class));
        Multibinder.newSetBinder(binder, TypeLiteral.get(ParamResolver.class));
        Multibinder.newSetBinder(binder, TypeLiteral.get(ResultTransformerFactory.class));
        Multibinder<RequestHandler> requestHandlers = Multibinder.newSetBinder(binder, TypeLiteral.get(RequestHandler.class));
        requestHandlers.addBinding().to(JaxRsRequestHandler.class);

//        binder.bind(BlockingResourceScheduler.class).toInstance(
//                new BlockingResourceScheduler(
//                        Schedulers.from(
//                                Executors.newFixedThreadPool(
//                                        serverConfig.getBlockingThreadPoolSize(),
//                                        new RxSyncThreadFactory()
//                                )
//                        )
//                )
//        );

        JaxRsServiceProviderImpl jaxRsServiceProvider = new JaxRsServiceProviderImpl();
        binder.bind(JaxRsServiceProvider.class).toInstance(jaxRsServiceProvider);

        for (Class<?> cls : injectAnnotatedScanner.getClasses()) {
            Optional<Class<?>> jaxRsClass = jaxRsServiceProvider.getJaxRsClass(cls);
            if (jaxRsClass.isPresent()) {
                Class<?> interfaceClass = jaxRsClass.get();
                jaxRsServiceProvider.addJaxRsResourceProvider(binder.getProvider(interfaceClass));
//                if (interfaceClass.isInterface()) {
//                    Provider<?> provider = binder.getProvider(cls);
//                    binder.bind(interfaceClass).toProvider(new ValidationProxyProvider(interfaceClass, provider));
//                }
            }
        }

    }


    private JavaTimeModule createJavaTimeModule() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        final DateTimeFormatter dateTimeFormatterAllowingSpace = new DateTimeFormatterBuilder().parseCaseInsensitive()
                .append(ISO_LOCAL_DATE)
                .optionalStart()
                .appendPattern("[[ ]['T']]")
                .append(ISO_LOCAL_TIME)
                .optionalEnd()
                .toFormatter();
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatterAllowingSpace));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateTimeFormatterAllowingSpace));

        DateTimeFormatter offsetOptionalColon = new DateTimeFormatterBuilder().parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart().appendOffset("+HH:MM:ss", "Z").optionalEnd()
                .optionalStart().appendOffset("+HHMMss", "Z").optionalEnd()
                .toFormatter();
        javaTimeModule.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer().with(offsetOptionalColon));
        return javaTimeModule;
    }

    /**
     * OffsetDateTime deserializer that does not adjust context timezone.
     */
    private static class OffsetDateTimeDeserializer extends InstantDeserializer<OffsetDateTime> {

        protected OffsetDateTimeDeserializer() {
            super(InstantDeserializer.OFFSET_DATE_TIME, false);
        }

        public JsonDeserializer<OffsetDateTime> with(DateTimeFormatter formatter) {
            return withDateFormat(formatter);
        }
    }
}