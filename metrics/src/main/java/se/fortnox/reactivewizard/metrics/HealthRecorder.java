package se.fortnox.reactivewizard.metrics;

import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to log and query healthyness.
 */
@Singleton
public class HealthRecorder {
    private final ConcurrentHashMap<Object, Boolean> statusPerMeasurement = new ConcurrentHashMap<>();
    private final AtomicBoolean                      healthy              = new AtomicBoolean(true);

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public boolean logStatus(Object key, boolean currentStatus) {
        final Boolean previousStatus = statusPerMeasurement.put(key, currentStatus);
        if (previousStatus == null || !previousStatus.equals(currentStatus)) {
            recalculateHealthyness();
        }
        return currentStatus;
    }

    private void recalculateHealthyness() {
        healthy.set(statusPerMeasurement.values().stream().allMatch(v -> v));
    }

    public boolean isHealthy() {
        return healthy.get();
    }
}
