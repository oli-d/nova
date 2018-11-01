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
HttpAdapter httpAdapter = HttpAdapter.builder().build();
```

The following parameters can or must be configured on the builder, so that a new instance can be created:

* ```identifier``` - Sets an identifier, which will be used for logging and metrics. Very useful if you plan to use 
multiple instances in your service.

* ```metrics``` - The ```Metrics``` instance used to capture request / response metrics. **Mandatory**

* ```httpServer``` - The server instance that should handle the (incoming) communication. 
  _Note: if you are using Spring, take a look at the [http-spring](./http-spring/README.md) package to 
  conveniently create a server instance_. **Mandatory**

* ```messageTranscriber``` - The object that is used to transform your messages from or to the wire format 
(```String```). If you do not provide your own transcriber, the
system tries to instantiate a default one. Default transcribers can be created
for ```String```, ```Integer```, ```Double``` and ```BigDecimal``` message types. For
all other message types, the implementation checks, whether the ```ObjectMapper``` from
the ```com.fasterxml.jackson``` library can be found on the classpath. If so,
all messages will be transcribed to/from a JSON string using a new instance of the ```ObjectMapper```. If ```ObjectMapper```
cannot be found, the system will only be able to automatically transcribe the above mentioned message types. For
all others, you have to register your own transcribers.

* ```defaultRequestTmeout``` - Defines the maximum time to wait for a reply, before timing out. This is an optional 
parameter, default is 30 seconds

With this information it is easy to see that the above example will NOT successfully create a new instance of
an ```HttpAdapter```. Instead, the builder would throw an exception, complaining that the
mandatory paramters have not been specified. The proper example would look something like this:

```
HttpAdapter httpAdapter = HttpAdapter.builder()
    .setIdentifier("MyService.Http")
    .setHttpServer(httpServer)
    .setDefaultRequestTimeout(20, TimeUnit.SECONDS)
    .setMetrics(myMetrics)
    .build();
```

### 3. Usage

The ```HttpAdapter``` can be used for both sides of an RPC, i.e. you can either use it on the client side to send 
a request to a server, or it can be used to build the server side, processing incoming client requests.
 
#### 3.1. The client view

The most simple method for an RPC client is 

```
Single<RpcReply<ReplyMessageType>> sendGetRequest(String destination, Class<ReplyMessageType> replyMessageType)
```

and its overloaded version 

```
Single<RpcReply<ReplyMessageType>> sendGetRequest(String destination, Class<ReplyMessageType> replyMessageType, long timeout, TimeUnit timeUnit)
```

Those methods allow you to send an HTTP GET request to the passed origin and return a ```Single``` which either
contains the server reply or an eventual error. The return value is created by the ```messageTranscriber``` you passed 
into the```HttpAdapter```'s builder, which will be fed the HTTP response body.

If you invoke the method without ```timeout``` and ```timeUnit``` parameters (or pass ```null``` for both) the adapter's
default timeout will be used.

Similar convenience methods exist to send POST, PUT and delete requests. There's also overloaded versions that allow
you to provide your own transcriber functions (for requests and replies), in case you do not want to use the default
ones.

#### 3.2. The server view

For the RPC server side, the ```HttpAdapter``` offers only a single method:

```
public <T> Flowable<RpcInvocation<T>> requests(String destination, Class<T> requestType)
```

With this method you can retrieve a ```Flowable``` of all incoming RPC request. The ```destination``` you pass in should
be relative to your ```HttpAdapter```'s base URL. E.g. if your ```HttpAdapter``` listens to 
```http://0.0.0.0:12345``` and you want to process all requests sent to ```http://0.0.0.0:12345/myEndpoint```, just 
invoke the method like this:
    
```
requests ("/myEndpoint", String.class)
```

The objects you retrieve from the ```Flowable``` are of type ```RpcInvocation```. Those objects offer the public final 
field ```request``` which represent the incoming request objects. 

If you want to send a response to an incoming request, invoke one of the overloaded  ```complete(...)``` methods of
```RpcInvocation```.

To illustrate this, here's an example of a super simple echo server:

```
httpAdapter.requests("/echo", String.class)
    .subscribe(
        invocation -> invocation.complete(invocation.request)
    );
```
