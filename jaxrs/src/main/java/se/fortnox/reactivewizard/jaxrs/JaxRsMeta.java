package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpMethod;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

public class JaxRsMeta {
    private HttpMethod method = null;
    private String produces = MediaType.APPLICATION_JSON;
    private boolean isProducesAnnotationPresent = false;
    private Consumes consumes = null;
    private final String fullPath;
    private boolean isDeprecated;

    private Stream.Type streamType;

    public JaxRsMeta(Method method) {
        this(method, null);
    }

    public JaxRsMeta(Method method, Path classPath) {
        Path methodPath = null;
        for (Annotation annotation : ReflectionUtil.getAnnotations(method)) {
            if (annotation instanceof GET) {
                this.method = HttpMethod.GET;
            } else if (annotation instanceof POST) {
                this.method = HttpMethod.POST;
            } else if (annotation instanceof PUT) {
                this.method = HttpMethod.PUT;
            } else if (annotation instanceof PATCH) {
                this.method = HttpMethod.PATCH;
            } else if (annotation instanceof DELETE) {
                this.method = HttpMethod.DELETE;
            } else if (annotation instanceof Produces) {
                isProducesAnnotationPresent = true;
                String[] types = ((Produces) annotation).value();
                if (types != null && types.length != 0) {
                    produces = types[0];
                }
            } else if (annotation instanceof Consumes) {
                consumes = (Consumes) annotation;
            } else if (annotation instanceof Path) {
                methodPath = (Path) annotation;
            } else if (annotation instanceof Stream annotationStream) {
                this.streamType = annotationStream.value();
            } else if (annotation instanceof Deprecated) {
                this.isDeprecated = true;
            }
        }

        // If the method was not deprecated, check for class level deprecation too
        if (!this.isDeprecated) {
            this.isDeprecated = isDeprecatedClass(method.getDeclaringClass());
        }

        if (classPath == null) {
            classPath = getPath(method.getDeclaringClass());
        }
        this.fullPath = concatPaths(classPath, methodPath);
    }

    /**
     * Get {@link Path} annotation from class or its interfaces.
     *
     * @param cls the class
     * @return The path annotation of the class or any of its interfaces
     */
    public static Path getPath(Class<?> cls) {
        Path path = cls.getAnnotation(Path.class);
        if (path != null) {
            return path;
        }

        for (Class<?> iface : cls.getInterfaces()) {
            path = iface.getAnnotation(Path.class);
            if (path != null) {
                return path;
            }
        }

        return null;
    }

    /**
     * Get {@link Deprecated} annotation from class or its interfaces.
     *
     * @param cls the class
     * @return If the class or any of its interfaces are deprecated
     */
    private static boolean isDeprecatedClass(Class<?> cls) {
        if (cls.getAnnotation(Deprecated.class) != null) {
            return true;
        }

        for (Class<?> iface : cls.getInterfaces()) {
            if (iface.getAnnotation(Deprecated.class) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Concatenate paths.
     *
     * @param path1 the first path
     * @param path2 the second path
     * @return the concatenated path
     */
    public static String concatPaths(Path path1, Path path2) {
        final String path1String = path1 == null ? "" : path1.value();
        final StringBuilder stringBuilder = new StringBuilder();
        if (!path1String.startsWith("/")) {
            stringBuilder.append("/");
        }
        stringBuilder.append(path1String);
        if (!path1String.isEmpty() && !path1String.endsWith("/")) {
            stringBuilder.append("/");
        }

        String path2String = path2 == null ? "" : path2.value();
        if (path2String.startsWith("/")) {
            path2String = path2String.substring(1);
        }
        stringBuilder.append(path2String);
        if (stringBuilder.charAt(stringBuilder.length() - 1) == '/') {
            return stringBuilder.substring(0, stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    public HttpMethod getHttpMethod() {
        return this.method;
    }

    public Stream.Type getStreamType() {
        return this.streamType;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getProduces() {
        return this.produces;
    }

    public Consumes getConsumes() {
        return this.consumes;
    }

    public String getFullPath() {
        return this.fullPath;
    }

    /**
     * Finds the JAX-RS class of a class, which may be the same class or an interface that it implements.
     *
     * @param cls is a class that might be a JaxRs resource
     * @return the JaxRs-annotated class, which might be the sent in class, or an interface implemented by it.
     */
    public static Optional<Class<?>> getJaxRsClass(Class<?> cls) {
        if (!cls.isInterface()) {
            if (cls.getAnnotation(Path.class) != null) {
                return Optional.of(cls);
            }

            for (Class<?> iface : cls.getInterfaces()) {
                if (iface.getAnnotation(Path.class) != null) {
                    return Optional.of(iface);
                }
            }
        }
        return Optional.empty();
    }

    public boolean isProducesAnnotationPresent() {
        return this.isProducesAnnotationPresent;
    }

    /**
     * @return If the endpoint has been annotated with {@link Deprecated}
     */
    public boolean isDeprecated() {
        return isDeprecated;
    }
}
