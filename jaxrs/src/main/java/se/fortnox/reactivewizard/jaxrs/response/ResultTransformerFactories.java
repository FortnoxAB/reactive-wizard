package se.fortnox.reactivewizard.jaxrs.response;

import se.fortnox.reactivewizard.jaxrs.JaxRsResource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class ResultTransformerFactories {

    private final ArrayList<ResultTransformerFactory> resultTransformerFactories;

    public ResultTransformerFactories(ResultTransformerFactory... resultTransformerFactories) {
        this(asList(resultTransformerFactories));
    }

    private ResultTransformerFactories(Collection<ResultTransformerFactory> resultTransformerFactories) {
        this.resultTransformerFactories = new ArrayList<>(resultTransformerFactories);
        this.resultTransformerFactories.sort(null);
    }

    @Inject
    public ResultTransformerFactories(Set<ResultTransformerFactory> resultTransformerFactories) {
        this((Collection<ResultTransformerFactory>) resultTransformerFactories);
    }

    public <T> ResultTransformer<T> createTransformers(JaxRsResource<T> resource) {
        List<ResultTransformer<T>> transformers = resultTransformerFactories
                .stream()
                .map(factory -> factory.create(resource))
                .filter(factory->factory!=null)
                .collect(Collectors.toList());

        return (result, args) -> {
            for (ResultTransformer<T> rp : transformers) {
                result = rp.apply(result, args);
            }
            return result;
        };
    }
}
