# annotation based RPC request handling for Nova

This package provides the ```OnRpcInvocation``` annotation that can be used
by a service to annotate an RPC handling method.

Let's look at an example:

```java
public class EchoService {
    ...

    @OnRpcInvocation(EchoRequest.class)
    public void handle (RpcInvocation<EchoRequest> echoRequestInvocation) {
        EchoResult result = computeResult();
        echoResultInvocation.complete(result);
    }
}
```

In that example, the method ```handle(...)``` is called whenever a new ```EchoRequest```
was received, computes an appropriate ```EchoResponse``` and returns it
to the client.

Note that the handler method must accept a single ```RpcInvocation``` parameter
(properly typed so that the request type matches the method annotation) and is not allowed
to return a result. Or in other word, it must be a ```Consumer<RpcInvocation<RequestTypeAccordingToAnnotation, ?, ?, ?>>```.

But how is this wired up? Where does the system read the incoming requests from?

The key component to answer those questions is the ```RpcRequestProcessor```. It is
available in Spring's ```ApplicationContext``` if you import the ```RpcRequestProcessorConfiguration```
in your service's configuration class. (_Note that in order to work properly, a ```Nova```
bean must exist in your ```ApplicationContext```. Either create it yourself or - for
more convenience - import ``` NovaProvidingConfiguration ``` from the module
[spring-support](../spring-support/README.md).)_

The ```RpcRequestProcessor``` is designed to work seamlessly with ```Nova```'s
communication adapters. It implements the interface ```io.reactivex.functions.Consumer```
so that processing all incoming requests from a specific adapter and destination is as simple
as subscribing the ```RpcRequestProcessor``` to the Flowable of incoming requests like so:

```java
    ...
    myCommAdapter
        .requests(requestDestination)
        .subscribe(rpcRequestProcessor)
    ...
```

The ```RpcRequestProcessor``` implements default behaviour for handling unregistered requests
as well as unexpected exceptions during processing. If you want to customize this behaviour,
you can define your own handler implementations by invoking
``` RpcRequestProcessor.onUnregisteredRequest() ``` resp. ``` RpcRequestProcessor.onProcessingException() ```

