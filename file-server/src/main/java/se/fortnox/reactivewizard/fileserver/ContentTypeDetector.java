package se.fortnox.reactivewizard.fileserver;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ContentTypeDetector {

    private static final Map<String, String> mimeTypes = new HashMap<>();

    static {
        mimeTypes.put("css", "text/css");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("js", "application/javascript");
    }

    private ContentTypeDetector() {
    }

    public static String getContentType(Path file) {
        return getContentType(file.toString());
    }

    public static String getContentType(String fileName) {
        return mimeTypes.getOrDefault(getExtension(fileName), "application/octet-stream");
    }

    private static String getExtension(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            int index = fileName.lastIndexOf('.');
            if (index >= 0) {
                return fileName.substring(index + 1);
            }
        }
        return "";
    }
}
