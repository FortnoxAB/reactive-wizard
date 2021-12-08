# Purpose of module
Implement a request handler capable of wrapping jaxrs-annotated paths, resolving jaxrs-annotated parameters,
and writing the response to the client.

#How

Collects all the different resources implemented in this codebase and puts them together in JaxRsResources class
which in turn is being used by the RequestHandler JaxRsRequestHandler

Each JaxRsResource represents an implementation of an interface method.

# Key classes

* `JaxRsRequestHandler` - handle incoming requests
* `JaxRsResources` - list of resources available and can find JaxRsResource to handle a certain request.
* `JaxRsResource` -> knows if it can handle a certain request and if so will resolve parameters from the request and call the implementation
  with the resolved parameters
* `JaxRsRequest` -> a wrapper around the incoming request with functionality to extract data from the request
