package se.fortnox.reactivewizard.test;

import org.junit.Assert;
import rx.Observable;
import rx.Subscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Small class for verifying things in a certain time.
 */
public abstract class WaitingTestUtils {

    private static final String DEFAULT_ERROR_MESSAGE = "Condition not met within time";
    private static final int DEFAULT_WAITING_SECONDS = 5;
    private static final TimeUnit DEFAULT_WAITING_TIME_UNIT = TimeUnit.SECONDS;

    private WaitingTestUtils() {}

    public static void assertConditionIsTrueWithin5Seconds(Supplier<Boolean> condition) {
        assertConditionIsTrueWithinTime(DEFAULT_WAITING_SECONDS, DEFAULT_WAITING_TIME_UNIT, condition, DEFAULT_ERROR_MESSAGE);
    }

    public static void assertConditionIsTrueWithin5Seconds(Supplier<Boolean> condition, String message) {
        assertConditionIsTrueWithinTime(DEFAULT_WAITING_SECONDS, DEFAULT_WAITING_TIME_UNIT, condition, message);
    }

    public static void assertConditionIsTrueWithinTime(int wait, TimeUnit waitingUnit, Supplier<Boolean> condition) {
        assertConditionIsTrueWithinTime(wait, waitingUnit, condition, DEFAULT_ERROR_MESSAGE);
    }

    public static void assertConditionIsTrueWithinTime(int wait, TimeUnit waitingUnit, Supplier<Boolean> condition, String message) {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        final Subscription subscribe = Observable
            .interval(100, MILLISECONDS)
            .takeUntil(aLong -> {
                final Boolean result = condition.get();
                if (Boolean.TRUE.equals(result)) {
                    countDownLatch.countDown();
                    return true;
                }
                return false;
            })
            .subscribe();

        try {
            final boolean await = countDownLatch.await(wait, waitingUnit);
            if (!await) {
                Assert.fail(message);
            }
        } catch (InterruptedException e) {
            Assert.fail();
        } finally {
            subscribe.unsubscribe();
        }
    }
}
