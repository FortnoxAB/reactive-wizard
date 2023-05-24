package se.fortnox.reactivewizard.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestUtilTest {

    @Test
    void shouldLoadTestManifest() {
        // Execution
        Optional<ManifestUtil.ManifestValues> manifestValuesOptional = ManifestUtil.getManifestValues();

        // Verification
        assertThat(manifestValuesOptional.isPresent()).isTrue();

        ManifestUtil.ManifestValues manifestValues = manifestValuesOptional.get();
        assertThat(manifestValues.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(manifestValues.getArtifactId()).isEqualTo("reactivewizard-utils");
    }
}
