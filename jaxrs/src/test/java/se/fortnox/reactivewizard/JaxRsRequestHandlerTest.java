package se.fortnox.reactivewizard;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;
import se.fortnox.reactivewizard.utils.JaxRsTestUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static rx.Observable.empty;
import static rx.Observable.error;

public class JaxRsRequestHandlerTest {

    @Test
    public void shouldRegisterMetricsForSuccessfulResponse() {
        assertCounterValue(NO_CONTENT, 0);

        TestResourceImpl testResource = new TestResourceImpl();
        MockHttpServerResponse x = JaxRsTestUtil.get(testResource, "/api/204");
        assertThat(x.getStatus()).isEqualTo(NO_CONTENT);

        assertCounterValue(NO_CONTENT, 1);
    }

    @Test
    public void shouldRegisterMetricsForFailingResponse() {
        assertCounterValue(INTERNAL_SERVER_ERROR, 0);

        TestResourceImpl testResource = new TestResourceImpl();
        MockHttpServerResponse x = JaxRsTestUtil.get(testResource, "/api/500");
        assertThat(x.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);

        assertCounterValue(INTERNAL_SERVER_ERROR, 1);
    }

    private void assertCounterValue(HttpResponseStatus status, int expectedValue) {
        long count = Metrics.registry()
            .counter("http_server_response_status:" + status.code())
            .getCount();
        assertThat(count).isEqualTo(expectedValue);
    }

    @Path("/api")
    public class TestResourceImpl {
        @GET
        @Path("204")
        public Observable<Void> get204() {
            return empty();
        }

        @GET
        @Path("500")
        public Observable<Void> get500() {
            return error(new WebException(INTERNAL_SERVER_ERROR));
        }
    }
}
