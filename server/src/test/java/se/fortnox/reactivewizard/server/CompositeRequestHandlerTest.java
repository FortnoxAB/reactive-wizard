package se.fortnox.reactivewizard.server;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.jaxrs.WebException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CompositeRequestHandlerTest {

    private CompositeRequestHandler compositeRequestHandler;
    private Set<RequestHandler> requestHandlers = new HashSet<>();

    @Mock
    private ExceptionHandler exceptionHandler;

    @Mock
    private HttpServerRequest request;

    @Mock
    private HttpServerResponse response;

    private ConnectionCounter connectionCounter;

    @Before
    public void beforeEach() {
        when(exceptionHandler.handleException(any(), any(), any())).thenReturn(Flux.empty());
        lenient().when(request.requestHeaders()).thenReturn(new DefaultHttpHeaders());
        lenient().when(response.responseHeaders()).thenReturn(new DefaultHttpHeaders());
        connectionCounter = new ConnectionCounter();
        compositeRequestHandler = new CompositeRequestHandler(requestHandlers, exceptionHandler, connectionCounter);
    }

    @Test
    public void shouldOnlyCallOneRequestHandler() {
        AtomicInteger callCounter = new AtomicInteger();
        requestHandlers.add((request, response) -> {
            callCounter.incrementAndGet();
            return Flux.empty();
        });
        requestHandlers.add((request, response) -> {
            callCounter.incrementAndGet();
            return Flux.empty();
        });

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        assertThat(callCounter.get()).isEqualTo(1);

    }

    @Test
    public void exceptionHandlerShallBeInvokedByRuntimeException() {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("expected exception");
        when(exceptionHandler.handleException(request, response, illegalArgumentException)).thenReturn(Flux.empty());

        requestHandlers.add((request, response) -> {
            throw illegalArgumentException;
        });

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        verify(exceptionHandler, times(1)).handleException(request, response, illegalArgumentException);
    }

    @Test
    public void exceptionHandlerShallBeInvokedWhenNoRequestHandlerIsGiven() {
        when(exceptionHandler.handleException(any(), any(), any())).thenReturn(Flux.empty());

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        verify(exceptionHandler, times(1)).handleException(any(), any(), any(WebException.class));
        assertThat(connectionCounter.getCount()).isEqualTo(0);
    }

    @Test
    public void exceptionHandlerShallBeInvokedWhenNullIsReturnedByRequestHandler() {

        when(response.status()).thenReturn(HttpResponseStatus.OK);
        requestHandlers.add((request, response) -> null);

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        verify(exceptionHandler, times(1)).handleException(any(), any(), any(WebException.class));

    }
}
