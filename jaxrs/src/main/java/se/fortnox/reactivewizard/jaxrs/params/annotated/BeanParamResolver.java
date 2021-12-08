package se.fortnox.reactivewizard.jaxrs.params.annotated;

import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolverFactories;
import se.fortnox.reactivewizard.json.Types;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

public class BeanParamResolver<T> extends AnnotatedParamResolver<T> {

    private static final Object NULL_VALUE = new Object();

    private final Function<JaxRsRequest, Mono<T>> resolver;

    public BeanParamResolver(Function<JaxRsRequest, Mono<T>> resolver) {
        super(null, null, null);
        this.resolver = resolver;
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return null;
    }

    @Override
    public Mono<T> resolve(JaxRsRequest request) {
        return resolver.apply(request);
    }

    public static class Factory implements AnnotatedParamResolverFactory {

        private final AnnotatedParamResolverFactories annotatedParamResolverFactories;

        public Factory(AnnotatedParamResolverFactories annotatedParamResolverFactories) {
            this.annotatedParamResolverFactories = annotatedParamResolverFactories;
        }

        @Override
        public <T> ParamResolver<T> create(TypeReference<T> paramType, Annotation annotation, String defaultValue) {
            //noinspection unchecked
            Class<T> beanParamCls = (Class<T>)paramType.getType();

            if (beanParamCls.isRecord()) {
                return createForRecord(beanParamCls);
            } else {
                return createForClass(beanParamCls);
            }
        }

        private <T> BeanParamResolver<T> createForClass(Class<T> beanParamCls) {
            Supplier<T> instantiator = ReflectionUtil.instantiator(beanParamCls);

            List<BiFunction<T, JaxRsRequest, Mono<T>>> fieldSetters = new ArrayList<>();

            for (Field field : getAllDeclaredFields(beanParamCls)) {
                Annotation[] fieldAnnotations = field.getAnnotations();
                for (Annotation fieldAnnotation : fieldAnnotations) {
                    AnnotatedParamResolverFactory paramResolverFactory = annotatedParamResolverFactories.get(fieldAnnotation.annotationType());
                    if (paramResolverFactory != null) {
                        TypeReference<T> fieldType = Types.toReference(field.getGenericType());
                        String defaultFieldValue = ParamResolverFactories.findDefaultValue(asList(fieldAnnotations));
                        ParamResolver<?> fieldResolver = paramResolverFactory.create(fieldType, fieldAnnotation, defaultFieldValue);

                        Optional<BiConsumer<T, Object>> setterOptional = ReflectionUtil.setter(beanParamCls, field.getName());
                        if (setterOptional.isEmpty()) {
                            continue;
                        }
                        BiConsumer<T, Object> setter = setterOptional.get();
                        fieldSetters.add((instance, request) -> {
                            Mono<?> fieldValue = fieldResolver.resolve(request);
                            return fieldValue.map(value -> {
                                setter.accept(instance, value);
                                return (T)instance;
                            });
                        });
                    }
                }
            }

            Function<JaxRsRequest, Mono<T>> resolver = (JaxRsRequest request) -> {
                T instance = instantiator.get();
                List<Mono<T>> runSetters = new ArrayList<>(fieldSetters.size());
                for (var setter : fieldSetters) {
                    runSetters.add(setter.apply(instance, request));
                }
                return Flux.merge(runSetters).count().map(count -> instance);
            };

            return new BeanParamResolver<>(resolver);
        }

        private <T> BeanParamResolver<T> createForRecord(Class<T> beanParamCls) {
            var constructors = beanParamCls.getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new IllegalArgumentException("A @BeanParam record may only have a single constructor");
            }

            var constructor = constructors[0];

            var constructorParams = constructor.getParameters();
            var constructorArgumentResolvers = new ArrayList<ParamResolver<Object>>(constructorParams.length);

            for (var constructorParam : constructorParams) {
                Annotation annotation = null;
                AnnotatedParamResolverFactory paramResolverFactory = null;

                var parameterAnnotations = asList(constructorParam.getAnnotations());

                for (var ann : parameterAnnotations) {
                    paramResolverFactory = annotatedParamResolverFactories.get(ann.annotationType());
                    if (paramResolverFactory != null) {
                        annotation = ann;
                        break;
                    }
                }

                if (paramResolverFactory == null) {
                    throw new IllegalArgumentException("Missing Param annotation for @BeanParam record parameter");
                }

                var paramType = Types.toReference(constructorParam.getParameterizedType());
                var defaultValue = ParamResolverFactories.findDefaultValue(parameterAnnotations);
                var paramResolver = paramResolverFactory.create(paramType, annotation, defaultValue);

                constructorArgumentResolvers.add(paramResolver);
            }

            Function<JaxRsRequest, Mono<T>> resolver = (JaxRsRequest request) -> {
                var argsFlux = Flux.fromIterable(constructorArgumentResolvers)
                    .map(it -> it.resolve(request).defaultIfEmpty(NULL_VALUE));

                return Flux.concat(argsFlux)
                    .reduce(new ArrayList<>(), (acc, next) -> {
                        if (next == NULL_VALUE) {
                            acc.add(null);
                        } else {
                            acc.add(next);
                        }
                        return acc;
                    })
                    .map(args -> {
                        try {
                            //noinspection unchecked
                            return (T)(constructor.newInstance(args.toArray()));
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    });
            };

            return new BeanParamResolver<>(resolver);
        }

        private static List<Field> getAllDeclaredFields(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                fields.addAll(asList(c.getDeclaredFields()));
            }
            return fields;
        }
    }
}
