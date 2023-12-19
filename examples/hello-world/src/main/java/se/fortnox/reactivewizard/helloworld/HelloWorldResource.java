package se.fortnox.reactivewizard.helloworld;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import reactor.core.publisher.Mono;

import static reactor.core.publisher.Mono.just;


@Path("/helloworld")
public class HelloWorldResource {

    @Inject
    public HelloWorldResource() {
    }

    @GET
    public Mono<String> greeting() {
        return just("Hello world!");
    }
}
