package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class JaxRsResources {
	private static final Logger log = LoggerFactory.getLogger(JaxRsResources.class);
	private List<JaxRsResource> resources;
	private final Object[] services;
	private boolean reloadClasses;
	private JaxRsResourceFactory jaxRsResourceFactory;

	public JaxRsResources(Object[] services, JaxRsResourceFactory jaxRsResourceFactory, Boolean classReloading) {
		this.services = services;
		this.reloadClasses = classReloading;
		this.jaxRsResourceFactory = jaxRsResourceFactory;

		this.resources = jaxRsResourceFactory.createResources(services);

		StringBuilder sb = new StringBuilder();
		for (JaxRsResource r : resources) {
			sb.append(System.lineSeparator());
			sb.append('\t');
			sb.append(r.toString());
		}
		log.info(sb.toString());
	}

	protected Observable<JaxRsResult<?>> call(HttpServerRequest<ByteBuf> request)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		if (reloadClasses) {
			resources = jaxRsResourceFactory.createResources(services);
		}

		JaxRsRequest jaxRsRequest = new JaxRsRequest(request);

		for (JaxRsResource r : resources) {
			Observable<JaxRsResult<?>> result = r.call(jaxRsRequest);
			if (result != null) {
				return result;
			}
		}

		return null;
	}
}
