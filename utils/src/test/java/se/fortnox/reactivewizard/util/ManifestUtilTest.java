package se.fortnox.reactivewizard.util;

import org.junit.Test;

import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;

public class ManifestUtilTest {

    @Test
    public void shouldLoadTestManifest() {
        // Execution
        Optional<ManifestUtil.ManifestValues> manifestValuesOptional = ManifestUtil.getManifestValues();

        // Verification
        assertThat(manifestValuesOptional.isPresent()).isTrue();

        ManifestUtil.ManifestValues manifestValues = manifestValuesOptional.get();
        assertThat(manifestValues.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(manifestValues.getArtifactId()).isEqualTo("reactivewizard-utils");
    }
}
