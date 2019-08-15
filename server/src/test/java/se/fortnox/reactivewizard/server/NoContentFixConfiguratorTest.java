package se.fortnox.reactivewizard.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class NoContentFixConfiguratorTest {

    @Test
    public void shouldOnlyRemoveHeaderContentLengthIfNoBody() throws Exception {
        DefaultHttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        httpResponse.headers().add("Content-Length", "0");
        httpResponse.headers().add("random", "0");

        NoContentFixConfigurator.NoContentBodyFix noContentFix = new NoContentFixConfigurator.NoContentBodyFix();
        noContentFix.write(mock(ChannelHandlerContext.class), httpResponse, null);
        assertThat(httpResponse.headers().size()).isEqualTo(1);
        assertThat(httpResponse.headers().get("random")).isEqualTo("0");

    }

    @Test
    public void shouldRemoveTransferEncodingIfNoContent() throws Exception {
        DefaultHttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
        httpResponse.headers().add("Transfer-Encoding", "some encoding");
        NoContentFixConfigurator.NoContentBodyFix noContentFix = new NoContentFixConfigurator.NoContentBodyFix();
        noContentFix.write(mock(ChannelHandlerContext.class), httpResponse, null);
        assertThat(httpResponse.headers().isEmpty()).isTrue();
    }

}
