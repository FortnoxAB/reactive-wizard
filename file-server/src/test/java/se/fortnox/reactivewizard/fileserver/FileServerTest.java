package se.fortnox.reactivewizard.fileserver;

import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.fest.assertions.Assertions.assertThat;

public class FileServerTest {

    FileServerConfig config = new FileServerConfig();

    @Test
    public void readsFile() {
        config = new FileServerConfig(".");
        assertThat(getResponse("/pom.xml")).startsWith("<project");
    }

    @Test
    public void willNotReadOutsideRoot() {
        config.setPath("./src");
        assertThat(getResponse("../pom.xml")).isNull();
    }

    @Test
    public void readsIndexFile() {
        config.setPath("./");
        config.setIndexFile("pom.xml");
        assertThat(getResponse("/")).isEqualTo(readFile("pom.xml"));
        assertThat(getResponse("/something-that-does-not-exits")).isEqualTo(readFile("pom.xml"));
        assertThat(getResponse("/src")).isEqualTo(readFile("pom.xml"));
    }

    @Test
    public void willNotServeBadIndexFile() {
        config.setPath("./");
        config.setIndexFile("derp.xml");
        assertThat(getResponse("/")).isNull();
    }

    @Test
    public void willNotServeFilesWhenPathPrefixDoesNotMatch() {
        config.setPath("./");
        config.setPathPrefix("/test-prefix");
        assertThat(getResponse("/pom.xml")).isNull();
    }

    @Test
    public void willServeFilesWhenPathPrefixMatches() {
        config.setPath("./");
        config.setPathPrefix("/test-prefix");
        assertThat(getResponse("/test-prefix/pom.xml")).isEqualTo(readFile("pom.xml"));
    }

    @Test
    public void willNotServeFilesWhenRootIsNull() {
        config.setPath(null);
        assertThat(getResponse("/pom.xml")).isNull();
    }

    @Test
    public void servesEmptyFileInsteadOfFavicon() {
        config.setPath("./");
        assertThat(getResponse("/favicon.ico")).isEqualTo("");
    }

    @Test
    public void correctContentType() {
        config.setPath("./src/test/resources");
        MockHttpServerResponse response = getFullResponse("/testfile.js");
        assertThat(response.getHeader("Content-Type")).isEqualTo("application/javascript; charset=utf-8");

        response = getFullResponse("/no_extension_file");
        assertThat(response.getHeader("Content-Type")).isEqualTo("application/octet-stream");
    }

    @Test
    public void sendsCustomHeaders() {
        config.setPath("./");
        config.setHeaders(new HashMap<String,String>(){{
            put("custom-header", "custom-value");
        }});
        MockHttpServerResponse response = getFullResponse("/pom.xml");
        assertThat(response.getHeader("custom-header")).isEqualTo("custom-value");
    }


    private String readFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getResponse(String uri) {
        MockHttpServerResponse response = getFullResponse(uri);
        if (response == null) {
            return null;
        }
        return response.getOutp();
    }

    private MockHttpServerResponse getFullResponse(String uri) {
        FileServerRequestHandler fileServer = new FileServerRequestHandler(config);
        MockHttpServerResponse response = new MockHttpServerResponse();
        Observable<Void> result = fileServer.handle(new MockHttpServerRequest(uri), response);
        if (result == null) {
            return null;
        }
        result.count().toBlocking().single();
        return response;
    }

}
