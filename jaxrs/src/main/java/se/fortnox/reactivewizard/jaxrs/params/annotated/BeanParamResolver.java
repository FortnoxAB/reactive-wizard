package se.fortnox.reactivewizard.jaxrs.params.annotated;

import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
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
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static rx.Observable.merge;
import static se.fortnox.reactivewizard.util.rx.FirstThen.first;

public class BeanParamResolver<T> extends AnnotatedParamResolver<T> {

    private final Supplier<T> instantiator;
    private final List<BiFunction<T, JaxRsRequest, Mono<T>>> fieldSetter;

    public BeanParamResolver(Supplier<T> instantiator, List<BiFunction<T, JaxRsRequest, Mono<T>>> setters) {
        super(null, null, null);
        this.instantiator = instantiator;
        this.fieldSetter = setters;
    }

    @Override
    protected String getValue(JaxRsRequest request) {
        return null;
    }

    @Override
    public Mono<T> resolve(JaxRsRequest request) {
        T instance = instantiator.get();
        List<Mono<T>> runSetters = new ArrayList<>(fieldSetter.size());
        for (int i = 0; i < fieldSetter.size(); i++) {
            runSetters.add(fieldSetter.get(i).apply(instance, request));
        }
        return Flux.merge(runSetters).count().map(count -> instance);
    }

    public static class Factory implements AnnotatedParamResolverFactory {

        private final AnnotatedParamResolverFactories annotatedParamResolverFactories;

        public Factory(AnnotatedParamResolverFactories annotatedParamResolverFactories) {
            this.annotatedParamResolverFactories = annotatedParamResolverFactories;
        }

        @Override
        public <T> ParamResolver<T> create(TypeReference<T> paramType, Annotation annotation, String defaultValue) {
            Class<T> beanParamCls = (Class<T>) paramType.getType();
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
                        if (!setterOptional.isPresent()) {
                            continue;
                        }
                        BiConsumer setter = setterOptional.get();
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
            return new BeanParamResolver<T>(instantiator, fieldSetters);
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
