package io.reactivex.netty.protocol.http.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.channel.ConnectionInputSubscriberEvent;
import io.reactivex.netty.protocol.http.client.internal.HttpClientToConnectionBridge;
import rx.subscriptions.Subscriptions;

/**
 * Essentially a bugfix for RxNetty. If we get a timeout in RxJava, resulting in a unsubscribe, when the request has
 * been sent, but the response headers have not been received, then the connection must be marked as DISCARDED. If not,
 * the connection will be returned to the pool and the response will be sent to the next user of the pooled connection.
 */
public class UnsubscribeAwareHttpClientToConnectionBridge extends HttpClientToConnectionBridge {
    @Override
    protected ConnectionInputSubscriber newConnectionInputSubscriber(ConnectionInputSubscriberEvent orig, Channel channel) {
        ConnectionInputSubscriber toReturn = super.newConnectionInputSubscriber(orig, channel);
        orig.getSubscriber().add(Subscriptions.create(()->{
            // Unsubscribed. Need to check if we are in the state of waiting for the response from the server.
            if (!toReturn.getState().receiveStarted()) {
                onClosedBeforeReceiveComplete(channel);
            }
        }));
        return toReturn;
    }

    public static void configurePipeline(ChannelPipeline pipeline) {
        pipeline.replace("HttpClientToConnectionBridge#0", UnsubscribeAwareHttpClientToConnectionBridge.class.getName(), new UnsubscribeAwareHttpClientToConnectionBridge());
    }
}
