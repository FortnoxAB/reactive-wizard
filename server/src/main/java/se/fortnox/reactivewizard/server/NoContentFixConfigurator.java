package se.fortnox.reactivewizard.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.HttpHandlerNames;
import rx.functions.Action1;

/**
 * Adds a fix for 204 NoContent and 1XX responses, which should have neither body nor Content-Length
 * header. Responses having "Content-Length: 0" are also having this header stripped because
 * https://tools.ietf.org/html/rfc2616#section-4.3 states that "The presence of a message-body in a request is signaled
 * by the inclusion of a Content-Length or Transfer-Encoding header field in the request's message-headers.", which
 * suggests that the inclusion of the header implies a body, which makes "Content-Length: 0" an invalid header.
 */
public class NoContentFixConfigurator implements Action1<ChannelPipeline> {

    private final int maxInitialLineLength;
    private final int maxChunkSize;
    private final int maxHeaderSize;

    public NoContentFixConfigurator(int maxInitialLineLength, int maxChunkSize, int maxHeaderSize) {
        this.maxInitialLineLength = maxInitialLineLength;
        this.maxChunkSize = maxChunkSize;
        this.maxHeaderSize = maxHeaderSize;
    }

    @Override
    public void call(ChannelPipeline pipeline) {
        pipeline.replace(
                HttpHandlerNames.HttpServerDecoder.getName(),
                HttpHandlerNames.HttpServerDecoder.getName(),
                new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize));
        pipeline.addAfter(HttpHandlerNames.HttpServerEncoder.getName(), "NoContentFix", new NoContentBodyFix());
    }

    /**
     * Removes both Transfer-Encoding and Content-Length headers for responses with status code 204.
     */
    public static class NoContentBodyFix extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof DefaultHttpResponse) {
                DefaultHttpResponse response = (DefaultHttpResponse) msg;
                boolean isEmptyBody = "0".equals(response.headers().get("Content-Length"));
                if (isEmptyBody) {
                    response.headers().remove("Content-Length");
                }
                HttpResponseStatus status = response.status();
                if (status.equals(HttpResponseStatus.NO_CONTENT) || status.code() < 200) {
                    response.headers().remove("Transfer-Encoding");
                }
            }
            super.write(ctx, msg, promise);
        }
    }
}

