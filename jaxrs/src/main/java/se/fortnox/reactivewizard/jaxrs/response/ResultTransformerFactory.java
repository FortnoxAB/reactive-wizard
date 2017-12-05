package se.fortnox.reactivewizard.jaxrs.response;

import se.fortnox.reactivewizard.jaxrs.JaxRsResource;

public interface ResultTransformerFactory extends Comparable<ResultTransformerFactory> {
    <T> ResultTransformer<T> create(JaxRsResource<T> resource);

    default Integer getPrio() {
        return 100;
    }

    @Override
    default int compareTo(ResultTransformerFactory resultTransformerFactory) {
        return this.getPrio().compareTo(resultTransformerFactory.getPrio());
    }

}
