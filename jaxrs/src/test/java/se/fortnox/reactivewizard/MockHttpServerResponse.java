package se.fortnox.reactivewizard;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.server.ServerMetricsEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockHttpServerResponse extends HttpServerResponse<ByteBuf> {

	private StringBuilder outp = new StringBuilder();

	public MockHttpServerResponse() {
		super(mockChannel(), mockSubject());
	}

	private static MetricEventsSubject<? extends ServerMetricsEvent<?>> mockSubject() {
		return new MetricEventsSubject<ServerMetricsEvent<?>>();
	}

	private static Channel mockChannel() {
		Channel channel = mock(Channel.class);
		EventExecutor eventExecutor = ImmediateEventExecutor.INSTANCE;
		ChannelPromise channelPromise = new DefaultChannelPromise(channel, eventExecutor);
		channelPromise.setSuccess();

		when(channel.alloc()).thenReturn(new UnpooledByteBufAllocator(true));
		when(channel.write(any())).thenReturn(channelPromise);
		when(channel.newPromise()).thenReturn(channelPromise);
		return channel;
	}

	private static ChannelHandlerContext mockChannelContext() {
		ChannelHandlerContext mock = mock(ChannelHandlerContext.class);
		Channel channel = mock(Channel.class);
		EventExecutor eventExecutor = ImmediateEventExecutor.INSTANCE;
		ChannelPromise channelPromise = new DefaultChannelPromise(channel, eventExecutor);
		channelPromise.setSuccess();

		when(mock.newPromise()).thenReturn(channelPromise);
		when(channel.alloc()).thenReturn(new PooledByteBufAllocator());
		when(channel.write(any())).thenReturn(channelPromise);
		when(channel.newPromise()).thenReturn(channelPromise);
		when(mock.channel()).thenReturn(channel);
		return mock;
	}

	@Override
	public void writeBytes(byte[] msg) {
		this.outp.append(new String(msg));
		super.writeBytes(msg);
	}

	@Override
	public void writeString(String msg) {
		this.outp.append(msg);
		super.writeString(msg);
	}

	public String getOutp() {
		return outp.toString();
	}

}
