package se.fortnox.reactivewizard.helloworld;

import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static rx.Observable.just;

@Path("/helloworld")
public class HelloWorldResource {

	@GET
	public Observable<String> greeting() {
		return just("Hello world!");
	}
}
