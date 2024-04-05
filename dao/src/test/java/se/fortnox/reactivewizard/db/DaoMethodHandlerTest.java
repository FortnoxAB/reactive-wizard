package se.fortnox.reactivewizard.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactory;
import se.fortnox.reactivewizard.metrics.Metrics;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.platform.commons.util.ReflectionUtils.getRequiredMethod;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DaoMethodHandlerTest {
    @Mock
    private PagingOutput pagingOutput;

    @Mock
    private DbStatementFactory dbStatementFactory;

    @Mock
    private ReactiveStatementFactory reactiveStatementFactory;

    private DaoMethodHandler daoMethodHandler;

    @Test
    void shouldReturnFluxForFluxReturnType() {
        var method = getRequiredMethod(TestDao.class, "selectFlux");
        setUpHandler(method);

        Publisher<Object> stmt = daoMethodHandler.create(new Object[0], reactiveStatementFactory);
        assertThat(stmt).isInstanceOf(Flux.class);
        verify(reactiveStatementFactory, atLeastOnce()).createFlux(any(), any(), any());
    }

    @Test
    void shouldReturnMonoForMonoReturnType() {
        var method = getRequiredMethod(TestDao.class, "selectMono");
        setUpHandler(method);

        Publisher<Object> stmt = daoMethodHandler.create(new Object[0], reactiveStatementFactory);
        assertThat(stmt).isInstanceOf(Mono.class);
        verify(reactiveStatementFactory, atLeastOnce()).createMono(any(), any());
    }

    @Test
    void shouldThrowExceptionForStringReturnType() {
        var method = getRequiredMethod(TestDao.class, "invalidSelect");
        setUpHandler(method);

        try {
            daoMethodHandler.create(new Object[0], reactiveStatementFactory);
            fail("Expected exception");
        } catch (Exception e) {
            assertThat(e.getMessage())
                .isEqualTo("DAO method se.fortnox.reactivewizard.db.DaoMethodHandlerTest$TestDao::invalidSelect must return a Flux or Mono. Found java.lang.String");
        }
    }

    private void setUpHandler(Method method) {
        daoMethodHandler = new DaoMethodHandler(
            method,
            dbStatementFactory,
            pagingOutput,
            Metrics.get("test")
        );
    }

    private interface TestDao {
        @Query("SELECT * FROM foo")
        Flux<String> selectFlux();

        @Query("SELECT * FROM foo")
        Mono<String> selectMono();

        @Query("SELECT * FROM foo")
        String invalidSelect();
    }
}
