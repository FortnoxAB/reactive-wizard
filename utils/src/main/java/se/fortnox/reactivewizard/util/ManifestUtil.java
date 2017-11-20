package se.fortnox.reactivewizard.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ManifestUtil {
    private static final String META_INF_ARTIFACT_ID = "ArtifactId";
    private static final String VERSION = "Version";

    private static ManifestValues manifestValues = loadAppManifestValues();

    public static class ManifestValues {
        private final String version;
        private final String artifactId;

        public ManifestValues(String version, String artifactId) {
            this.version = version;
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public String getArtifactId() {
            return artifactId;
        }
    }

    /**
     * @return version and artifactId values from the application manifest if present. <br>
     *         Can be absent when running not in "fat jar" with custom manifest (e.g. during local development)
     */
    public static Optional<ManifestValues> getManifestValues() {
        return Optional.ofNullable(manifestValues);
    }

    private static ManifestValues loadAppManifestValues() {
        Manifest manifest = loadAppManifest();
        if (manifest != null) {
            Attributes mainAttributes = manifest.getMainAttributes();
            String version = mainAttributes.getValue(VERSION);
            String artifactId = mainAttributes.getValue(META_INF_ARTIFACT_ID);
            return new ManifestValues(version, artifactId);
        } else {
            return null;
        }
    }

    private static Manifest loadAppManifest() {
        try {
            Enumeration<URL> resources = ManifestUtil.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (resources.hasMoreElements()) {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                if (manifest.getMainAttributes().getValue(META_INF_ARTIFACT_ID) != null) { // note "containsKey" doesn't work
                    return manifest;
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Cannot load manifest.", e);
        }
    }
}
