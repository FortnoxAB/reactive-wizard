# Purpose of module
Provide additional annotations missing in jaxrs and the standard ones from jaxrs

#Key classes
`WebException` - an exception with status code and error code meant to be used as return value when something goes wrong.
`Headers` - use this on your implementation to send custom headers.
`Stream` - use this to stream results to the client (good for large streams of byte arrays).
`PATCH` - from the time when PATCH was not in the jaxrs api itself.
`SuccessStatus` - define your own success status with this annotation.
