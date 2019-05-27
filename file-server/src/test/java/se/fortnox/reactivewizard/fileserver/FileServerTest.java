package se.fortnox.reactivewizard.fileserver;

import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import static org.fest.assertions.Assertions.assertThat;

public class FileServerTest {

    @Test
    public void readsFile() {
        FileServerConfig config = new FileServerConfig();
        config.setPath(".");
        FileServerRequestHandler fileServer = new FileServerRequestHandler(config);
        MockHttpServerResponse response = new MockHttpServerResponse();
        Observable<Void> result = fileServer.handle(new MockHttpServerRequest("/pom.xml"), response);
        assertThat(result).isNotNull();

        result.count().toBlocking().single();

        assertThat(response.getOutp()).startsWith("<project");
    }

    @Test
    public void willNotReadOutsideRoot() {
        FileServerConfig config = new FileServerConfig();
        config.setPath("./src");
        FileServerRequestHandler fileServer = new FileServerRequestHandler(config);
        MockHttpServerResponse response = new MockHttpServerResponse();
        Observable<Void> result = fileServer.handle(new MockHttpServerRequest("../pom.xml"), response);
        assertThat(result).isNull();
    }
}
