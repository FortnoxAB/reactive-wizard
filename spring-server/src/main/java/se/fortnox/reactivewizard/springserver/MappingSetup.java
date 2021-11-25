package se.fortnox.reactivewizard.springserver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class MappingSetup {

    @Inject
    public MappingSetup(@Named("args") String[] args) {
        MySpringApplication.getInstance(RwServerConfig.class).run(args);
    }
}
