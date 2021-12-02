package se.fortnox.reactivewizard.springserver.poc;

import reactor.core.publisher.Mono;
import rx.Observable;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/dummy")
public class DummyResourceImpl {

    @Inject
    public DummyResourceImpl() {
    }


    @POST
    @Path("{name}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Observable<String> test(@FormParam("formparam") String formValue, @PathParam("name") String name) {
        String message = String.format(
            "Hello %s FormParam received: %s",
            name,
            formValue
        );
        return Observable.just(message);
    }

    @POST
    @Path("formparam")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Observable<String> formParam(@FormParam("formparam") String formValue) {
        String message = String.format(
            "FormParam received: %s",
            formValue
        );
        return Observable.just(message);
    }

    @GET
    @Path("withqueryparam")
    public Mono<Map<String, Object>> testById(@QueryParam("name") String name) {
        return Mono.just(Map.of("Hello", "queryparam world " + name));
    }
}
