package se.fortnox.reactivewizard.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthRecorderTest {

    @Test
    void shouldReturnHealthyWhenStatusIsGood() {
        HealthRecorder healthRecorder = new HealthRecorder();
        healthRecorder.logStatus("ok", true);
        assertThat(healthRecorder.isHealthy()).isTrue();
    }

    @Test
    void shouldReturnHealthyWhenNoStatusIsRecorded() {
        HealthRecorder healthRecorder = new HealthRecorder();
        assertThat(healthRecorder.isHealthy()).isTrue();
    }

    @Test
    void shouldReturnUnhealthyWhenStatusIsBad() {
        HealthRecorder healthRecorder = new HealthRecorder();
        healthRecorder.logStatus("ok", false);
        assertThat(healthRecorder.isHealthy()).isFalse();
    }


    @Test
    void shouldRecalculateWhenStatusChanges() {
        HealthRecorder healthRecorder = new HealthRecorder();

        healthRecorder.logStatus("ok", false);
        assertThat(healthRecorder.isHealthy()).isFalse();

        healthRecorder.logStatus("ok", true);
        assertThat(healthRecorder.isHealthy()).isTrue();
    }
}
