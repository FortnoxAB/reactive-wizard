package se.fortnox.reactivewizard.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
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

    @Mock
    private RequestLogger requestLogger;

    @Captor
    private ArgumentCaptor<WebException> webExceptionCaptor;

    private ConnectionCounter connectionCounter;

    @BeforeEach
    public void beforeEach() {
        connectionCounter = new ConnectionCounter();
        compositeRequestHandler = new CompositeRequestHandler(requestHandlers, exceptionHandler, connectionCounter, requestLogger);
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

        assertThat(callCounter).hasValue(1);

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
        when(exceptionHandler.handleException(any(), any(), any())).thenReturn(Flux.empty());

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        verify(exceptionHandler).handleException(any(), any(), any(WebException.class));
        assertThat(connectionCounter.getCount()).isZero();
    }

    @Test
    void exceptionHandlerShallBeInvokedWhenNullIsReturnedByRequestHandler() {
        when(exceptionHandler.handleException(any(), any(), any(WebException.class))).thenReturn(Flux.empty());
        requestHandlers.add((request, response) -> null);

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        verify(exceptionHandler).handleException(any(), any(), any(WebException.class));

    }

    @Test
    void shouldReturnNotFoundWhenNoHandlersMatch() {
        when(exceptionHandler.handleException(any(), any(), any(WebException.class)))
            .thenReturn(Flux.empty());
        doNothing()
            .when(requestLogger).logRequestResponse(any(), any(), anyLong(), any());

        Flux.from(compositeRequestHandler.apply(request, response)).count().block();

        verify(exceptionHandler).handleException(any(), any(), webExceptionCaptor.capture());
        WebException handledException = webExceptionCaptor.getValue();
        assertThat(handledException)
            .hasFieldOrPropertyWithValue("status", NOT_FOUND)
            .hasFieldOrPropertyWithValue("error", "resource.not.found");
    }
}
