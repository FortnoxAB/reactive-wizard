package se.fortnox.reactivewizard.server;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.inject.Inject;

@RestController
public class DummySpringResource {
    @Inject
    public DummySpringResource() {
        System.out.println("Starting dummmy spring resource");
    }

    @RequestMapping("/spring/dummy")
    public Mono<String> getSpringDummy() {
        return Mono.just("Hello spring world");
    }
}
