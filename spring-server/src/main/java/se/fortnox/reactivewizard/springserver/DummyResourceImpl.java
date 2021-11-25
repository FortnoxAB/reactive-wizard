package se.fortnox.reactivewizard.springserver;

import reactor.core.publisher.Mono;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Map;

@Path("/dummy")
public class DummyResourceImpl {

    private final TestCache testCache;

    @Inject
    public DummyResourceImpl(TestCache testCache) {
        this.testCache = testCache;
    }

    @GET
    @Path("{name:\\d*}")
    public Mono<String> test(@PathParam("name") String name) {
        return Mono.just("hello " + name);
    }

    @GET
    @Path("withqueryparam")
    public Mono<Map<String, Object>> testById(@QueryParam("name") String name) {
        return Mono.just(Map.of("Hello", "queryparam world " + name));
    }
}
