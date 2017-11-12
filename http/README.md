nova-http [![Build Status](https://travis-ci.org/oli-d/nova-http.svg?branch=master)](https://travis-ci.org/oli-d/nova-http)
=========

RPC style communication over HTTP(S).

### 1. What is this about?
This package provides the ```HttpAdapter``` class which can be used to 
* send requests via HTTP to a remote server and wait for the result,

and

* listen to incoming HTTP requests and send an appropriate reply to the caller

In both cases "normal, synchronous, RPC-style" HTTP communication patterns are implemented.

### 2. HttpAdapter instantiation
The first thing you have to do is create a new ```HttpAdapter``` instance using its Builder:
 
```
HttpAdapter<String> httpAdapter = HttpAdapter.<String>builder().build();
```

Note that  ```HttpAdapter``` is a generic class. When instantiating, you specify the base class of
all messages being exchanged (incoming and outgoing). In the above example we work with Strings.

The following parameters can or must be configured on the builder, so that a new instance can be created:

* ```identifier``` - Sets an identifier, which will be used for logging and metrics. Very useful if you plan to use 
multiple instances in your service.

* ```metrics``` - The ```Metrics``` instance used to capture request / response metrics. **Mandatory**

* ```httpServer``` - The server instance that should handle the (incoming) communication. 
  _Note: if you are using Spring, take a look at the [http-spring](./http-spring/README.md) package to 
  conveniently create a server instance_. **Mandatory**

* ```messageMarshaller``` - The function used to transform your messages to the wire format (always ```String``` in our example)
before they are sent out. If you do not specify your own marshaller, the
system tries to instantiate a default one. Default marshallers can be created
for ```String```, ```Integer```, ```Double``` and ```BigDecimal``` message types. For
all other message types, the implementation checks, whether the ```ObjectMapper``` from
the ```com.fasterxml.jackson``` library can be found on the classpath. If so,
all messages will be marshalled to a JSON string before sent. If the ```ObjectMapper```
cannot be found, the system gives up and throws an error. All default marshallers
transform the messages to a String.

* ```messageUnmarshaller``` - The function used to transform incoming messages (```String```) to your internal 
representation. If you do not specify your own unmarshaller, the
system tries to instantiate a default one. Default unmarshallers can be created
for ```String```, ```Integer```, ```Double``` and ```BigDecimal``` message types. For
all other message types, the implementation checks, whether the ```ObjectMapper``` from
the ```com.fasterxml.jackson``` library can be found on the classpath. If so,
all messages will be unmarshalled from a JSON string to the specific type. If the ```ObjectMapper```
cannot be found, the system gives up and throws an error. All default unmarshallers
expect the incoming messages to be represented as a String.

* ```errorReplyFactory``` - The function used to transform an exception that occurred during request processing to 
an outgoing reply messages. **Mandatory**

* ```defaultRequestTmeout``` - Defines the maximum time to wait for a reply, before timing out. This is an optional 
parameter, default is 30 seconds

With this information it is easy to see that the above example will NOT successfully create a new instance of
a ```String``` based ```HttpAdapter```. Instead, the builder would throw an exception, complaining that the
mandatory paramters have not been specified. The proper example would look something like this:

```
HttpAdapter<String> httpAdapter = HttpAdapter.<String>builder()
    .setIdentifier("MyService.Http")
    .setHttpServer(httpServer)
    .setDefaultRequestTimeout(20, TimeUnit.SECONDS)
    .setMessageMarshaller(s -> s)
    .setMessageUnmarshaller(s -> s)
    .setMetrics(myMetrics)
    .setErrorReplyFactory(t -> "Error: " + t)
    .build();
```

### 3. Usage

The ```HttpAdapter``` can be used for both sides of an RPC, i.e. you can either use it on the client side to send 
a request to a server, or it can be used to build the server side, processing incoming client requests.
 
#### 3.1. The client view

The most simple method for an RPC client is 

```
Single<ReplyType> sendGetRequest(String destination)
```

and its overloaded version 

```
Single<ReplyType> sendGetRequest(String destination, long timeout, TimeUnit timeUnit)
```

Those methods allow you to send an HTTP GET request to the passed destination and return a ```Single``` which either 
contains the server reply or an eventual error. The return value is created by the ```messageUnmarshaller``` you passed 
into the```HttpAdapter```'s builder, which will be fed the HTTP response body.

If you invoke the method without ```timeout``` and ```timeUnit``` parameters (or pass ```null``` for both) the adapter's
default timeout will be used.

Similar methods exist to send a POST request:
```
Single<ReplyType> sendPostRequest(String destination, RequestType request)
Single<ReplyType> sendPostRequest(String destination, RequestType request, long timeout, TimeUnit timeUnit)
```

Note that for POST it is expected that a payload will be sent with the request. This payload will
be transferred in the HTTP request body. It will be marshalled to a String by passing the ```request``` object
to the ```HttpAdapter```'s ```messageMarshaller```.

And there are also methods to send a PUT request
```
Single<ReplyType> sendPutRequest(String destination, RequestType request)
Single<ReplyType> sendPutRequest(String destination, RequestType request, long timeout, TimeUnit timeUnit)
```
which behave exactly like their POST counterparts.

The most versatile of the client methods is 
```
public Single<ReplyType> sendRequest (String destination, RequestType request, HttpSpecificInfo httpInfo, Long timeout, TimeUnit timeUnit)
```

In fact, all the previously mentioned methods are just convenience methods that internally invoke this one. Using this 
method you have full control over your request. The parameters are
* ```destination``` - the URL you want to send the request to
* ```request type``` - the object that should be sent in the HTTP request body. Must be a subtype of the type, your 
```HttpAdapter``` was parameterized with 
* ```httpInfo``` - HTTP protocol specific sending information, containing
  * ```requestMethod``` - either ```HttpRequestMethod.GET``` or ```HttpRequestMethod.POST``` 
* ```timeout``` - the amount of time, the system should wait for a reply before a Timeout exception is raised. Can be null,
in which case the default timeout is applied.
* ```timeUnit``` - the unit, the ```timeout``` value is expressed in. Must not be ```null``` if ```timeout``` is not ```null```

#### 3.2. The server view

For the RPC server side, the ```HttpAdapter``` offers only a single method:

```
public Flowable<RpcInvocation> requests (String destination, BackpressureStrategy backpressureStrategy)
```

With this method you can retrieve a ```Flowable``` of all incoming RPC request. The ```destination``` you pass in should
be relative to your ```HttpAdapter```'s base URL. E.g. if your ```HttpAdapter``` listens to 
```http://0.0.0.0:12345``` and you want to process all requests sent to ```http://0.0.0.0:12345/myEndpoint```, just 
invoke the method like this:
    
```
requests ("/myEndpoint", myBackpressureStrategy)
```

The objects you retrieve from the ```Flowable``` are of type ```RpcInvocation```. Those objects offer the public final 
field ```request``` which represent the incoming HTTP request body. In addition to that, the methods   

```
void complete(ReplyType reply)
void completeExceptionally(Throwable error)
```

are exposed. You can use them to send back an appropriate reply resp. error to the caller.

To illustrate this, here's an example of a simple echo server:

```
httpAdapter.requests("/echo", BackpressureStrategy.BUFFER)
    .subscribe(
        invocation -> invocation.complete(invocation.request)
    );
```
