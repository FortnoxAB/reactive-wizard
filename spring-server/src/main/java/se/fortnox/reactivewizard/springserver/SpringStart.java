package se.fortnox.reactivewizard.springserver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Purpose of this class is to start an already created application context only when the guice environment is ready so that our spring beans
 * can have dependencies on RW resources.
 */
@Singleton
public class SpringStart {

    @Inject
    public SpringStart(@Named("args") String[] args) {
        RWSpringApplication.getInstance(RwServerConfig.class).run(args);
    }
}
