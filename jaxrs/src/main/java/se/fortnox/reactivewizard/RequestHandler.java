package se.fortnox.reactivewizard;

import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.function.BiFunction;

public interface RequestHandler extends BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
}
