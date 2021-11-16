package se.fortnox.reactivewizard.server;

import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import rx.functions.Action0;
import se.fortnox.reactivewizard.RequestHandler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

import static java.util.Arrays.asList;
import static reactor.netty.channel.BootstrapHandlers.updateConfiguration;

/**
 * Runs an Reactor @{@link HttpServer} with all registered @{@link RequestHandler}s.
 */
@Component
@EnableAutoConfiguration
public class RwServer2 implements CommandLineRunner {


    @Override
    public void run(String... args) throws Exception {
        main(args);
    }


    public static void main(String[] args) {
        //Logic
        System.out.println("hello");


    }





}
