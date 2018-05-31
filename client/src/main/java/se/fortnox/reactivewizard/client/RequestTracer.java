package se.fortnox.reactivewizard.client;

import com.google.common.base.Strings;
import se.fortnox.reactivewizard.util.ManifestUtil;

import java.util.Objects;

/**
 * Creates an identifier for this system based on host, artifact and
 * version, which is added to outgoing requests for tracing purposes.
 */
public class RequestTracer {

    private static final String HTTP_X_VIA_CLIENT_HEADER = "X-Via-Client";

    private final String appIdentifier;

    public RequestTracer(ManifestUtil.ManifestValues manifestValues, String hostName) {
        Objects.requireNonNull(manifestValues);
        appIdentifier = createAppIdentifier(manifestValues, hostName);
    }

    private static String createAppIdentifier(ManifestUtil.ManifestValues manifestValues, String hostName) {
        return String.format("%s:%s:%s",
            hostName,
            manifestValues.getArtifactId(),
            manifestValues.getVersion());
    }

    public void addTrace(RequestBuilder request) {
        String referrer  = request.getHeaders().get(HTTP_X_VIA_CLIENT_HEADER);
        String viaClient = appIdentifier;
        if (!Strings.isNullOrEmpty(referrer)) {
            viaClient = referrer + "," + appIdentifier;
        }
        request.addHeader(HTTP_X_VIA_CLIENT_HEADER, viaClient);
    }
}
