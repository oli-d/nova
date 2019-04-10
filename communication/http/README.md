http
=========

RPC style communication over HTTP(S).

## 1. What is this about?
This package provides the ```HttpAdapter``` class which can be used to 
* send requests via HTTP to a remote server and wait for the result,

and

* listen to incoming HTTP requests and send an appropriate reply to the caller

In both cases "normal, synchronous, RPC-style" HTTP communication patterns are implemented.

## 2. HttpAdapter instantiation

### 2.1 Using Spring

The easiest way to retrieve an ```HttpAdapter``` instance is using Spring. For this, we provide
the class ```HttpEnablingConfiguration```. Simply import this in your own Spring configuration
and you can get a bean called "httpAdapter" from the ApplicationContext.

When you do so, default configuration will be applied, which can be overridden via
environment variables or by providing the appropriate beans yourself. The possible
configuration values are

  | @Bean name                         | Environnment variable name                   | Description                                              | Default value |
  |------------------------------------|----------------------------------------------|----------------------------------------------------------|---------------|
  | httpAdapterIdentifier              | NOVA.HTTP.ADAPTER_IDENTIFIER                 | the identifier to assign to the HttpAdapter.             | <null> |
  | defaultHttpRequestTimeoutInSeconds | NOVA.HTTP.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS | the default timeout in seconds when firing HTTP requests | 30 |
  | autoCreateHttpServer               | NOVA.HTTP.SERVER.AUTO_CREATE                 | If this is false, the system will NOT automatically create an HttpServer | true |
  | autoStartHttpServer                | NOVA.HTTP.SERVER.AUTO_FALSE                  | If true, the system will automatically start the HttpServer when the ApplicationContext is initialized| true |
  | | | | |
  | httpServer                         | n/a                                          | the ```HttpServer``` instance, handling the incoming communication. | An instance with default settings (see below) |
  | httpObjectMapper                   | n/a                                          | the ObjectMapper to use when transcribing incoming / outgoing messages| default ObjectMapper, for details see [here](../comm/README.md) |
  | httpMessageTranscriber             | n/a                                          | the transcriber to use for incoming / outgoing messages  | default transcriber, for details see [here](../comm/README.md) |

As described above, by default the HttpAdapter will use an ```HttpServer``` with default settings. This bean is provided by the  
```HttpServerProvidingConfiguration``` which is automatically imported. The configuration options are: 
   
  | @Bean name                         | Environnment variable name                   | Description                                              | Default value |
  |------------------------------------|----------------------------------------------|----------------------------------------------------------|---------------|
  | httpServerPort                     | NOVA.HTTP.SERVER.PORT                        | the port, the HTTP server listens on                     | 10000         |
  | httpServerInterfaceName            | NOVA.HTTP.SERVER.INTERFACE_NAME              | the interface, the HTTP server listens on                | "0.0.0.0"     |
  | httpServerKeyStore                 | NOVA.HTTP.SERVER.KEY_STORE                   | the keystore to use. Switches on SSL                     | <null>        |
  | httpServerKeyStorePass             | NOVA.HTTP.SERVER.KEY_STORE_PASS              | the password for the keystore                            | <null>        |
  | httpServerTrustStore               | NOVA.HTTP.SERVER.TRUST_STORE                 | the truststore to use to validate clients                | <null>        |
  | httpServerTrustStorePass           | NOVA.HTTP.SERVER.TRUST_STORE_PASS            | the password for the trust store                         | <null>        |
  | | | | |
  | httpServerSettings                 | n/a                                          | an ```HttpServerSettings``` instance, containing all aforementioned config values. Handy if you want to read the configuration or override multiple defaults programmatically. |  |
   
Note that the ```HttpServer``` is not mandatory. The adapter can be created without it in case
you want to work in "client mode" only. 
    
### 2.2 Manual instantiation

To create a new ```HttpAdapter``` instance manually, you need to do this via its Builder:
 
```
HttpAdapter httpAdapter = HttpAdapter.builder().build();
```

The following parameters can or must be configured on the builder, so that a new instance can be created:

* ```identifier``` - Sets an identifier, which will be used for logging and metrics. Very useful if you plan to use 
multiple instances in your service.

* ```metrics``` - The ```Metrics``` instance used to capture request / response metrics. **Mandatory**

* ```httpServer``` - The server instance that should handle the (incoming) communication. 
  _Note: if you are using Spring, take a look at the [http-ch.squaredesk.nova.spring](./http-spring/README.md) package to 
  conveniently create a server instance_. **Mandatory**

* ```messageTranscriber``` - The object that is used to transform your messages from or to the wire format 
(```String```). If you do not provide your own transcriber, the
system tries to instantiate a default one. 

* ```defaultRequestTmeout``` - Defines the maximum time to wait for a reply, before timing out. This is an optional 
parameter, default is 30 seconds

With this information it is easy to see that the above example will NOT successfully create a new instance of
an ```HttpAdapter```. Instead, the builder would throw an exception, complaining that the
mandatory parameters have not been specified. The proper example would look something like this:

```
HttpAdapter httpAdapter = HttpAdapter.builder()
    .setIdentifier("MyService.Http")
    .setHttpServer(httpServer)
    .setDefaultRequestTimeout(20, TimeUnit.SECONDS)
    .setMetrics(myMetrics)
    .build();
```

## 3. Usage

The ```HttpAdapter``` can be used for both sides of an RPC, i.e. you can either use it on the client side to send 
a request to a server, or it can be used to build the server side, processing incoming client requests.
 
### 3.1. The client view

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

### 3.2. The server view

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


