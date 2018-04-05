package se.fortnox.reactivewizard.metrics;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class HealthRecorderTest {

    @Test
    public void shouldReturnHealthyWhenStatusIsGood() {
        HealthRecorder healthRecorder = new HealthRecorder();
        healthRecorder.logStatus("ok", true);
        assertThat(healthRecorder.isHealthy()).isTrue();
    }

    @Test
    public void shouldReturnHealthyWhenNoStatusIsRecorded() {
        HealthRecorder healthRecorder = new HealthRecorder();
        assertThat(healthRecorder.isHealthy()).isTrue();
    }

    @Test
    public void shouldReturnUnhealthyWhenStatusIsBad() {
        HealthRecorder healthRecorder = new HealthRecorder();
        healthRecorder.logStatus("ok", false);
        assertThat(healthRecorder.isHealthy()).isFalse();
    }


    @Test
    public void shouldRecalculateWhenStatusChanges() {
        HealthRecorder healthRecorder = new HealthRecorder();

        healthRecorder.logStatus("ok", false);
        assertThat(healthRecorder.isHealthy()).isFalse();

        healthRecorder.logStatus("ok", true);
        assertThat(healthRecorder.isHealthy()).isTrue();
    }
}
