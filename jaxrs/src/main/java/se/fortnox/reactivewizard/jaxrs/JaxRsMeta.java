package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.util.ReflectionUtil;
import io.netty.handler.codec.http.HttpMethod;

import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class JaxRsMeta {
	private HttpMethod method = null;
	private Path methodPath = null;
	private String produces = MediaType.APPLICATION_JSON;
	private Consumes consumes = null;
	private String fullPath;

	public JaxRsMeta(Method m) {
		this(m, null);
	}

	public JaxRsMeta(Method m, Path clsPath) {
		for (Annotation a : ReflectionUtil.getAnnotations(m)) {
			if (a instanceof GET) {
				method = HttpMethod.GET;
			} else if (a instanceof POST) {
				method = HttpMethod.POST;
			} else if (a instanceof PUT) {
				method = HttpMethod.PUT;
			} else if (a instanceof PATCH) {
				method = HttpMethod.PATCH;
			} else if (a instanceof DELETE) {
				method = HttpMethod.DELETE;
			} else if (a instanceof Produces) {
				String[] types = ((Produces) a).value();
				if (types != null && types.length != 0) {
					produces = types[0];
				}
			} else if (a instanceof Consumes) {
				consumes = (Consumes)a;
			} else if (a instanceof Path) {
				methodPath = (Path) a;
			}
		}
		if (clsPath == null) {
			clsPath = getPath(m.getDeclaringClass());
		}
		fullPath = concatPaths(clsPath, methodPath);
	}

	public HttpMethod getHttpMethod() {
		return method;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	public String getProduces() {
		return produces;
	}

	public Consumes getConsumes() {
		return consumes;
	}

	public String getFullPath() {
		return fullPath;
	}

	public static Path getPath(Class<? extends Object> cls) {
		Path path = cls.getAnnotation(Path.class);
		if (path == null) {
			for (Class<?> i : cls.getInterfaces()) {
				path = i.getAnnotation(Path.class);
				if (path != null) {
					return path;
				}
			}
		}
		return path;
	}

	public static String concatPaths(Path p1, Path p2) {
		String p1s = p1 == null ? "" : p1.value();
		String p2s = p2 == null ? "" : p2.value();
		StringBuilder sb = new StringBuilder();
		if (!p1s.startsWith("/")) {
			sb.append("/");
		}
		sb.append(p1s);
		if (!p1s.endsWith("/")) {
			sb.append("/");
		}
		if (p2s.startsWith("/")) {
			p2s = p2s.substring(1);
		}
		sb.append(p2s);
		if (sb.charAt(sb.length() - 1) == '/') {
			return sb.substring(0, sb.length() - 1);
		}
		return sb.toString();
	}

}
