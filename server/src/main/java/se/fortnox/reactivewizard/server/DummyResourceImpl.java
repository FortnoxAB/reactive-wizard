package se.fortnox.reactivewizard.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import rx.Single;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.time.Duration;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;

@Path("/dummy")
@Slf4j
@Controller
public class DummyResourceImpl {

    static final String SERVER = "Server";
    static final String RESPONSE = "Response";

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
