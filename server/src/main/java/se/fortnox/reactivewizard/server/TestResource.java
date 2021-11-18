package se.fortnox.reactivewizard.server;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("api")
public class TestResource {

    @Inject
    public TestResource() {

    }

    @Path("stuff")
    @GET
    public Mono<String> doStuff() {
        return Mono.just("hello");
    }
}
