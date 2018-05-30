package se.fortnox.reactivewizard.client;

import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.pool.PoolConfig;
import io.reactivex.netty.client.pool.PoolLimitDeterminationStrategy;
import rx.Observable;

/**
 * Bugfix for RxNetty, which will not release a connection pool permit if the connection observable is unsubscribed
 * (from a timeout) before the connection is returned or before an error occurs.
 */
public class UnsubscribeAwareConnectionProviderFactory implements ConnectionProviderFactory<ByteBuf, ByteBuf> {
    private final ConnectionProviderFactory<ByteBuf, ByteBuf> wrapped;
    private final PoolConfig<ByteBuf, ByteBuf> poolConfig;

    public UnsubscribeAwareConnectionProviderFactory(ConnectionProviderFactory<ByteBuf, ByteBuf> wrapped, PoolConfig<ByteBuf,ByteBuf> poolConfig) {
        this.wrapped = wrapped;
        this.poolConfig = poolConfig;
    }
    @Override
    public ConnectionProvider<ByteBuf, ByteBuf> newProvider(Observable<HostConnector<ByteBuf, ByteBuf>> hosts) {
        return new UnsubscribeAwareConnectionProvider(wrapped.newProvider(hosts), poolConfig.getPoolLimitDeterminationStrategy());
    }

    private static class UnsubscribeAwareConnectionProvider implements ConnectionProvider<ByteBuf, ByteBuf> {
        private final ConnectionProvider<ByteBuf, ByteBuf> wrapped;
        private final PoolLimitDeterminationStrategy poolLimitDeterminationStrategy;

        public UnsubscribeAwareConnectionProvider(ConnectionProvider<ByteBuf, ByteBuf> wrapped, PoolLimitDeterminationStrategy poolLimitDeterminationStrategy) {
            this.wrapped = wrapped;
            this.poolLimitDeterminationStrategy = poolLimitDeterminationStrategy;
        }

        @Override
        public Observable<Connection<ByteBuf, ByteBuf>> newConnectionRequest() {
            AtomicBoolean connectionReturned = new AtomicBoolean();
            return wrapped.newConnectionRequest()
                    .doOnNext(c->connectionReturned.set(true))
                    .doOnError(e->connectionReturned.set(true))
                    .doOnUnsubscribe(()->{
                        if (!connectionReturned.get()) {
                            // Unsubscribed before a connection was returned. Must give back a permit.
                            poolLimitDeterminationStrategy.releasePermit();
                        }
                    });
        }
    }
}
