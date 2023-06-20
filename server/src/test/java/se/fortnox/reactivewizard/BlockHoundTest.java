package se.fortnox.reactivewizard;

import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatException;

class BlockHoundTest {
    @Test
    void blockHoundShouldDetectBlockingCall() {
        assertThatException()
            .isThrownBy(() -> Mono.delay(Duration.ofMillis(1))
                .doOnNext(it -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .block())
            .withCauseInstanceOf(BlockingOperationError.class);
    }
}
