package se.fortnox.reactivewizard.springserver.poc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@RestController
@ResponseBody
public class DummySpringResource {

    @GetMapping("/spring/dummy/{name}")
    public Mono<String> getSpringDummyPathParam(@PathParam("name") String pathParam) {
        return Mono.just("Hello spring world " + pathParam);
    }

    @GetMapping("/spring/dummy")
    public Mono<String> getSpringDummyQueryParam(@QueryParam("name") String queryParam) {
        return Mono.just("Hello spring world " + queryParam);
    }
}
