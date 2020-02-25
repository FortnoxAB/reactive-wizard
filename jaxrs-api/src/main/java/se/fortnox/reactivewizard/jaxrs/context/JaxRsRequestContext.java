package se.fortnox.reactivewizard.jaxrs.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A context that exists during handling of a jaxrs request.
 *
 * Any user code that wishes to interact with the context should do so using the {@link #setValue(Object, Object)} and
 * {@link #getValue(Object)} methods.
 */
public class JaxRsRequestContext {
    private static final Logger                           LOG     = LoggerFactory.getLogger(JaxRsRequestContext.class);
    private static final ThreadLocal<JaxRsRequestContext> CONTEXT = new ThreadLocal<>();

    private final Map<Object, Object> context;

    private JaxRsRequestContext() {
        context = new ConcurrentHashMap<>();
    }

    static JaxRsRequestContext getContext() {
        return CONTEXT.get();
    }

    static void setContext(JaxRsRequestContext context) {
        CONTEXT.set(context);
    }

    /**
     * Retrieve a value stored in the context.
     *
     * @param key The key associated with the value
     * @param <T> The value type
     * @return An Optional containing the value. The Optional is empty if there is no value set,
     *         if the value set is null or if the context is not open.
     */
    public static <T> Optional<T> getValue(Object key) {
        JaxRsRequestContext ctx = getContext();
        if (ctx == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) ctx.context.get(key));
    }

    /**
     * Store a value in the context.
     *
     * @param key   The key associated with the value
     * @param value The value
     */
    public static void setValue(Object key, Object value) {
        JaxRsRequestContext ctx = getContext();
        if (ctx == null) {
            LOG.warn("No context exists or this is not executed in the context of a request");
            return;
        }
        ctx.context.put(key, value);
    }

    /**
     * Open a new context.
     *
     * The context does not support reentry and will throw an exception if a context already exists.
     * User code should not need to open a context.
     *
     * @throws IllegalStateException if a context already exists
     */
    public static void open() {
        if (CONTEXT.get() != null) {
            throw new IllegalStateException("A context already exists");
        }
        CONTEXT.set(new JaxRsRequestContext());
    }

    /**
     * Close the existing context.
     *
     * Closing the context will remove it and all stored properties will become unavailable.
     * User code should not need to close a context.
     */
    public static void close() {
        if (CONTEXT.get() == null) {
            return;
        }
        CONTEXT.remove();
    }
}
