package se.fortnox.reactivewizard.jaxrs.context;

import io.reactiverse.reactivecontexts.core.ContextProvider;

public class JaxRsRequestContextProvider implements ContextProvider<JaxRsRequestContext> {
    @Override
    public JaxRsRequestContext install(JaxRsRequestContext jaxRsRequestContext) {
        JaxRsRequestContext previous = JaxRsRequestContext.getContext();
        JaxRsRequestContext.setContext(jaxRsRequestContext);
        return previous;
    }

    @Override
    public void restore(JaxRsRequestContext jaxRsRequestContext) {
        JaxRsRequestContext.setContext(jaxRsRequestContext);
    }

    @Override
    public JaxRsRequestContext capture() {
        return JaxRsRequestContext.getContext();
    }
}
