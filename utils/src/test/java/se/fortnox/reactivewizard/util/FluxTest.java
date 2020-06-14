package se.fortnox.reactivewizard.util;

import org.junit.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class FluxTest {

    @Test
    public void testUnsubscribe() {
        Flux.interval(Duration.ofSeconds(1))
            .map(i->{
//                if (i == 3) {
//                    throw new RuntimeException("NOT 3");
//                }
                return i;
            })
            .take(5)
            .doFinally(i->System.out.println("finally"))
            .doOnTerminate(()-> System.out.println("terminate"))
            .doOnCancel(()-> System.out.println("cancel"))
            .take(10)
            .doOnNext(i-> System.out.println("i: "+i))
            .blockLast();
    }
}
