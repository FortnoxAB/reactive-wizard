package se.fortnox.reactivewizard.server;

import org.junit.Test;
import rx.Observable;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionCounterTest {

	ConnectionCounter connectionCounter = new ConnectionCounter();

	@Test
	public void testZeroConnections() {
		assertTrue(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
	}

	@Test
	public void testOneConnection() {
		connectionCounter.increase();
		connectionCounter.decrease();
		assertTrue(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
	}

	@Test
	public void testTwoConnections() {
		connectionCounter.increase();
		connectionCounter.increase();
		connectionCounter.decrease();
		connectionCounter.decrease();
		assertTrue(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
	}

	@Test
	public void testOneNeverEndingConnection() {
		connectionCounter.increase();
		assertFalse(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
	}

	@Test
	public void testOneDelayedConnection() {
		connectionCounter.increase();
		delayed(()->connectionCounter.decrease());
		assertFalse(connectionCounter.awaitZero(0, TimeUnit.SECONDS));
		assertTrue(connectionCounter.awaitZero(10, TimeUnit.SECONDS));
	}

	private void delayed(Runnable function) {
		Observable.fromCallable(()->{
			function.run();
			return true;
		}).delaySubscription(500, TimeUnit.MILLISECONDS).subscribe();
	}

}
