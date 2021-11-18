package se.fortnox.reactivewizard.server;

import rx.Observable;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static rx.Observable.just;

@Path("/dummy")
public class DummyResourceImpl {

    @Inject
    public DummyResourceImpl() {
    }

    @GET
    public Observable<String> test() {
        return just("Hello world");
    }
}
