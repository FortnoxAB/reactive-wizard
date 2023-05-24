package se.fortnox.reactivewizard.server;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.jaxrs.WebException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeRequestHandlerTest {

    private CompositeRequestHandler compositeRequestHandler;
    private final Set<RequestHandler> requestHandlers = new HashSet<>();

    @Mock
    private ExceptionHandler exceptionHandler;

    @Mock
    private HttpServerRequest request;

    @Mock
    private HttpServerResponse response;

    private ConnectionCounter connectionCounter;

    @BeforeEach
    public void beforeEach() {
        connectionCounter = new ConnectionCounter();
        compositeRequestHandler = new CompositeRequestHandler(requestHandlers, exceptionHandler, connectionCounter, new RequestLogger());
    }

    @Test
    void shouldOnlyCallOneRequestHandler() {
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
    void exceptionHandlerShallBeInvokedByRuntimeException() {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("expected exception");
        when(exceptionHandler.handleException(request, response, illegalArgumentException)).thenReturn(Flux.empty());

        requestHandlers.add((request, response) -> {
            throw illegalArgumentException;
        });

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        verify(exceptionHandler).handleException(request, response, illegalArgumentException);
    }

    @Test
    void exceptionHandlerShallBeInvokedWhenNoRequestHandlerIsGiven() {
        when(response.status()).thenReturn(null);
        when(exceptionHandler.handleException(any(), any(), any())).thenReturn(Flux.empty());

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        verify(exceptionHandler).handleException(any(), any(), any(WebException.class));
        assertThat(connectionCounter.getCount()).isZero();
    }

    @Test
    void exceptionHandlerShallBeInvokedWhenNullIsReturnedByRequestHandler() {

        when(response.status()).thenReturn(HttpResponseStatus.OK);
        when(exceptionHandler.handleException(any(), any(), any(WebException.class))).thenReturn(Flux.empty());
        requestHandlers.add((request, response) -> null);

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        verify(exceptionHandler).handleException(any(), any(), any(WebException.class));

    }
}
