package se.fortnox.reactivewizard.helloworld;

import reactor.core.publisher.Mono;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
