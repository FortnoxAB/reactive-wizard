package se.fortnox.reactivewizard.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
    public Mono<String> test(@PathParam("name") String name) {
        return Mono.just("hello " + name);
    }

    @GET
    @Path("withqueryparam")
    public Mono<Map<String, Object>> testById(@QueryParam("name") String name) {
        return Mono.just(Map.of("Hello", "queryparam world " + name));
    }

    @GET
    @Path("waitalongtime")
    public Mono<String> waitALongTime() {
        return Mono.just("Hejsan efter en stund")
            .delayElement(Duration.of(10, SECONDS));
    }

    @MessageMapping("request-response")
    public Mono<RSocketMessage> requestResponse(RSocketMessage request) {
        log.info("Received request-response request: {}", request);
        // create a single Message and return it
        return Mono.just(new RSocketMessage(SERVER, RESPONSE));
    }
}
