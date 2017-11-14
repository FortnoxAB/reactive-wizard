package se.fortnox.reactivewizard.helloworld;

import io.reactivex.netty.RxNetty;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;

public class Main {

	public static void main(String[] args) {

		RxNetty.createHttpServer(8080,
			new JaxRsRequestHandler(new HelloWorldResource())).startAndWait();
	}
}
