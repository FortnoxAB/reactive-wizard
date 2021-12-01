package se.fortnox.reactivewizard.springserver;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class SpringStart {

    @Inject
    public SpringStart(@Named("args") String[] args) {
        MySpringApplication.getInstance(RwServerConfig.class).run(args);
    }
}
