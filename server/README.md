# Purpose
Start the server responsible for handling all incoming requests.

# Key classes
* `RwServer` - the server thread which will keep the system alive.
* `ServerConfig` - the configuration of the server.
* `CompositeRequestHandler` - knows every RequestHandler that you register and will redirect calls to them and will return the result from 
whichever RequestHandler returns a result != null

# Notes
Register your own requesthandler by implementing the `RequestHandler` interface and in an implementation of `AutobindModule` do
```
Multibinder<RequestHandler> requestHandlerMultibinder = Multibinder.newSetBinder(binder, new TypeLiteral<RequestHandler>() {
});
requestHandlerMultibinder.addBinding().to(TracingJaxRsRequestHandler.class);
```

inside the `configure` method
