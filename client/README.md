# Purpose of module
Provide non implemented @Path-annotated java interfaces with a default implementation to call a dedicated host. 
This enables subsystems to be able to @Inject these interfaces without implementation.

# How
The java interface methods must return either `Observable` or `Single` and be typed with the expected body structure.
If you expect a String the return type must be `Observable<String>`

The server that the standard http calls are directed to is configured through the httpClient configuration in your configuration file.
See `HttpClientConfig` for configurable properties with url being the minimum requirement like this:

`httpClient: https://www.google.com`

Different HttpClientConfigs can be used in different resources by annotating the subclass of HttpClientConfig with:  
`@UseInResource([ResourceToUseAParticularConfig.class])`

Normally only the body of the response is returned, if you need the entire response back use the static methods of HttpClient.

`HttpClient.getFullResponse(httpResource.getData())`

Which will return a response where you can inspect the response more closely alongside with the deserialized body.

# Key classes

`HttpClient` - where all the logic happenes when calling a jaxrs-annotated interface.
`HttpClientConfig` - configure the HttpClient to be used with base url being the minimum requirement.

