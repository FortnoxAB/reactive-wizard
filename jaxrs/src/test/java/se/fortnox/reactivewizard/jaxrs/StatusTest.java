package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.MockHttpServerRequest;
import se.fortnox.reactivewizard.MockHttpServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import org.junit.Test;
import rx.Observable;

import javax.ws.rs.*;

import static org.fest.assertions.Assertions.assertThat;

public class StatusTest {

	ExceptionHandler exceptionHandler = new ExceptionHandler();
	JaxRsRequestHandler handler = new JaxRsRequestHandler(new Object[] { new TestresourceImpl() },
			new JaxRsResourceFactory(),
			exceptionHandler,
			false);

	@Test
	public void shouldReturn200ForGET_PUT_PATCH() {
		assertStatus("/test/get", HttpMethod.GET, HttpResponseStatus.OK);
		assertStatus("/test/put", HttpMethod.PUT, HttpResponseStatus.OK);
		assertStatus("/test/patch", HttpMethod.PATCH, HttpResponseStatus.OK);
	}

	@Test
	public void shouldReturn201ForPOST() {
		assertStatus("/test/post", HttpMethod.POST, HttpResponseStatus.CREATED);
		assertStatus("/test/postWithQueryAndBody",
				HttpMethod.POST,
				HttpResponseStatus.CREATED,
				"{\"name\":\"hej\"}");
	}

	@Test
	public void shouldReturn204ForDELETE() {
		assertStatus("/test/delete", HttpMethod.DELETE, HttpResponseStatus.NO_CONTENT);
	}

	@Test
	public void returnsGivenStatusWithSuccessStatusAnnotation() {
		assertStatus("/test/post-custom", HttpMethod.POST, HttpResponseStatus.OK);
	}

	private void assertStatus(String url, HttpMethod m, HttpResponseStatus status) {
		assertStatus(url, m, status, null);
	}

	private void assertStatus(String url, HttpMethod m, HttpResponseStatus status, String body) {
		HttpServerRequest<ByteBuf> request = new MockHttpServerRequest(url, m, body);
		MockHttpServerResponse response = new MockHttpServerResponse();
		Observable<Void> result = handler.handle(request, response);
		result.onErrorReturn(e -> {
			exceptionHandler.handleException(request, response, e);
			return null;
		}).toBlocking().singleOrDefault(null);
		assertThat(response.getStatus()).isEqualTo(status);
	}

	class TestresourceImpl implements TestresourceInterface {

		@Override
		public Observable<String> get() {
			return Observable.just("get");
		}

		@Override
		public Observable<String> put() {
			return Observable.just("put");
		}

		@Override
		public Observable<String> post() {
			return Observable.just("post");
		}

		@Override
		public Observable<String> patch() {
			return Observable.just("patch");
		}

		@Override
		public Observable<Void> delete() {
			return Observable.empty();
		}

		@Override
		public Observable<String> postCustom() {
			return Observable.just("post-custom");
		}

		@Override
		public Observable<String> postWithQueryAndBody(Integer validatedInt, ParamEntity param) {
			return Observable.just("post ok");
		}
	}

	@Path("test")
	public interface TestresourceInterface {
		@Path("get")
		@GET
		Observable<String> get();

		@Path("put")
		@PUT
		Observable<String> put();

		@Path("post")
		@POST
		Observable<String> post();

		@Path("patch")
		@PATCH
		Observable<String> patch();

		@Path("postWithQueryAndBody")
		@POST
		Observable<String> postWithQueryAndBody(@QueryParam("validInt") Integer validatedInt, ParamEntity param);

		@Path("delete")
		@DELETE
		Observable<Void> delete();

		@Path("post-custom")
		@POST
		@SuccessStatus(200)
		Observable<String> postCustom();
	}
}
