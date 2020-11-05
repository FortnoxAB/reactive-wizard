package se.fortnox.reactivewizard.test;

import org.junit.Assert;
import rx.Observable;
import rx.Subscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Small class for verifying things in a certain time.
 */
public abstract class WaitingTestUtils {

    private static final String DEFAULT_ERROR_MESSAGE = "Condition not met within time";
    private static final int DEFAULT_WAITING_SECONDS = 5;
    private static final TimeUnit DEFAULT_WAITING_TIME_UNIT = TimeUnit.SECONDS;

    private WaitingTestUtils() {}

    public static void assertConditionIsTrueWithin5Seconds(BooleanSupplier condition) {
        assertConditionIsTrueWithinTime(DEFAULT_WAITING_SECONDS, DEFAULT_WAITING_TIME_UNIT, condition, DEFAULT_ERROR_MESSAGE);
    }

    public static void assertConditionIsTrueWithin5Seconds(BooleanSupplier condition, String message) {
        assertConditionIsTrueWithinTime(DEFAULT_WAITING_SECONDS, DEFAULT_WAITING_TIME_UNIT, condition, message);
    }

    public static void assertConditionIsTrueWithinTime(int wait, TimeUnit waitingUnit, BooleanSupplier condition) {
        assertConditionIsTrueWithinTime(wait, waitingUnit, condition, DEFAULT_ERROR_MESSAGE);
    }

    public static void assertConditionIsTrueWithinTime(int wait, TimeUnit waitingUnit, BooleanSupplier condition, String message) {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        final Subscription subscribe = Observable
            .interval(100, MILLISECONDS)
            .takeUntil(aLong -> {
                final boolean result = condition.getAsBoolean();

                if (result) {
                    countDownLatch.countDown();
                }

                return result;
            })
            .subscribe();

        try {
            final boolean await = countDownLatch.await(wait, waitingUnit);
            if (!await) {
                Assert.fail(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assert.fail();
        } finally {
            subscribe.unsubscribe();
        }
    }
}
