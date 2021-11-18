package se.fortnox.reactivewizard.server;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Map;

@Path("/dummy")

//Todo shouldn't be necessary with this annotation
@ResponseBody
public class DummyResourceImpl {

    @Inject
    public DummyResourceImpl() {
    }

    @GET
    @Path("{name}")
    public Mono<Map<String, Object>> test(@PathParam("name") String name) {
        return Mono.just(Map.of("Hello", "pathparam World" + name));
    }

    @GET
    @Path("withqueryparam")
    public Mono<Map<String, Object>> testById(@QueryParam("name") String name) {
        return Mono.just(Map.of("Hello", "queryparam world " + name));
    }
}
