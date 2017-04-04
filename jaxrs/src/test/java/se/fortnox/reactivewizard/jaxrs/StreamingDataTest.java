package se.fortnox.reactivewizard.jaxrs;

import org.junit.Test;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static se.fortnox.reactivewizard.jaxrs.JaxRsTestUtil.body;
import static se.fortnox.reactivewizard.jaxrs.JaxRsTestUtil.get;
import static org.fest.assertions.Assertions.assertThat;
import static rx.Observable.just;

/**
 * Created by jonashall on 2015-12-02.
 */
public class StreamingDataTest {

	@Test
	public void shouldSupportStreamingData() {
		assertThat(body(get(new StreamingResource(), "/"))).isEqualTo("ab");
	}

	@Path("")
	class StreamingResource {
		@GET
		@Stream
		@Produces(MediaType.TEXT_PLAIN)
		public Observable<String> streamOfStrings() {
			return just("a", "b");
		}
	}
}
