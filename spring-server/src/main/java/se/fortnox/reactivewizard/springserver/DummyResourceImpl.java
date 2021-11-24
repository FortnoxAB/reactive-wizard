package se.fortnox.reactivewizard.springserver;

import rx.Single;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Map;

@Path("/dummy")
public class DummyResourceImpl {

    @Inject
    public DummyResourceImpl() {
    }

    @GET
    @Path("{name:\\d*}")
    public Single<String> test(@PathParam("name") String name) {
        return Single.just("hello " + name);
    }

    @GET
    @Path("withqueryparam")
    public Single<Map<String, Object>> testById(@QueryParam("name") String name) {
        return Single.just(Map.of("Hello", "queryparam world " + name));
    }
}
