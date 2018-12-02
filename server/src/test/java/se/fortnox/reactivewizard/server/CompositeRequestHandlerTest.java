package se.fortnox.reactivewizard.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.WebException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class CompositeRequestHandlerTest {

    private CompositeRequestHandler compositeRequestHandler;
    private Set<RequestHandler<ByteBuf, ByteBuf>> requestHandlers = new HashSet<>();

    @Mock
    private ExceptionHandler exceptionHandler;

    @Mock
    private HttpServerRequest<ByteBuf> request;

    @Mock
    private HttpServerResponse<ByteBuf> response;


    private ConnectionCounter connectionCounter;


    @Before
    public void beforeEach() {
        when(exceptionHandler.handleException(any(), any(), any())).thenReturn(Observable.empty());
        connectionCounter = new ConnectionCounter();
        compositeRequestHandler = new CompositeRequestHandler(requestHandlers, exceptionHandler, connectionCounter);
    }

    @Test
    public void shouldOnlyCallOneRequestHandler() {
        AtomicInteger callCounter = new AtomicInteger();
        requestHandlers.add((request, response) -> {
            callCounter.incrementAndGet();
            return Observable.empty();
        });
        requestHandlers.add((request, response) -> {
            callCounter.incrementAndGet();
            return Observable.empty();
        });

        compositeRequestHandler.handle(request, response).toBlocking().firstOrDefault(null);

        assertThat(callCounter.get()).isEqualTo(1);

    }

    @Test
    public void exceptionHandlerShallBeInvokedByRuntimeException() {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("expected exception");
        when(exceptionHandler.handleException(request, response, illegalArgumentException)).thenReturn(Observable.empty());

        requestHandlers.add((request, response) -> {
            throw illegalArgumentException;
        });

        compositeRequestHandler.handle(request, response).test();

        verify(exceptionHandler, times(1)).handleException(request, response, illegalArgumentException);
    }

    @Test
    public void exceptionHandlerShallBeInvokedWhenNoRequestHandlerIsGiven() {
        when(exceptionHandler.handleException(any(), any(), any())).thenReturn(Observable.empty());
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("expected exception");
        when(exceptionHandler.handleException(request, response, illegalArgumentException)).thenReturn(Observable.empty());

        compositeRequestHandler.handle(request, response).test().awaitTerminalEvent();

        verify(exceptionHandler, times(1)).handleException(any(), any(), any(WebException.class));
        assertThat(connectionCounter.getCount()).isEqualTo(0);
    }

    @Test
    public void exceptionHandlerShallBeInvokedWhenNullIsReturnedByRequestHandler() {

        when(response.getStatus()).thenReturn(HttpResponseStatus.OK);
        requestHandlers.add((request, response) -> null);

        compositeRequestHandler.handle(request, response).toBlocking().firstOrDefault(null);

        verify(exceptionHandler, times(1)).handleException(any(), any(), any(WebException.class));

    }
}
