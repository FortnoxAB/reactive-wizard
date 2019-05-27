package se.fortnox.reactivewizard.fileserver;

import se.fortnox.reactivewizard.config.Config;

import java.util.HashMap;
import java.util.Map;

@Config("static-files")
public class FileServerConfig {

    private String path;
    private Map<String, String> headers	= new HashMap<String, String>();
    private String indexFile;
    private String pathPrefix;

    public FileServerConfig() {
    }

    public FileServerConfig(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getIndexFile() {
        return indexFile;
    }

    public void setIndexFile(String indexFile) {
        this.indexFile = indexFile;
    }

    /**
     * An optional prefix in the url that is removed before evaluating the path.
     *
     * Allows you to have a url like this:
     * /foo/test.html
     *
     * That is mapped to a file:
     * /test.html
     *
     * @return path prefix
     */
    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }
}
