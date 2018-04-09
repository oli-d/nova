# annotation based RPC request handling for Nova

This package provides the ```OnRpcInvocation``` annotation that can be used
by a service to annotate an RPC handling method.

Let's look at an example:

```java
public class EchoService {
    ...

    @OnRpcInvocation(EchoRequest.class)
    public EchoResponse handle (EchoRequest echoRequest) {
        ...
    }
}
```

In that example, the method ```handle(...)``` is called whenever a new ```EchoRequest```
was received, computes and returns an appropriate ```EchoResponse```. Please note
that the handler method must accept a single parameter of the type that was defined
in the annotation value and always return a result. Or in other word, it must be
a function from the declared request type to the result type.

But how is this wired up? Where does the system read the incoming requests from? How can we
send the computed result back to the client?

The key component to answer those questions is the ```RpcRequestProcessor```. It is
available in Spring's ```ApplicationContext``` if you import the ```RpcRequestProcessorConfiguration```
in your service's configuration class.

The ```RpcRequestProcessor``` is designed to work seamlessly with ```Nova```'s
communication adapters. It implements the interface ```io.reactivex.functions.Function```
so that processing all incoming requests from a specific adapter is as simple as
mapping the incoming request object to the corresponding service reply:

```java
    ...
    myCommAdapter
        .requests(requestDestination)
        .map(rpcRequestProcessor)
    ...
```

The result of this ```map()``` operation is a ```Flowable``` of request/result pairs
that can be subscribed to for further processing (e.g. to send an
appropriate reply message back to the client):

```java
    ...
    myCommAdapter
        .requests(requestDestination)
        .map(rpcRequestProcessor)
        .subscribe(requestReplyPair -> requestReplyPair._1.complete(requestReplyPair._2))
    ...
```


processes the incoming requests, checks whether an appropriate handler was registered
and - if so - invokes it.

This feature is implemented using a specific Spring ```BeanPostProcessor```. Therefore, to be able to use
the functionality described above,
__the following beans must exist in the ApplicationContext:__
1. ___All of your beans that have been annotated___

1. ___A Nova instance (called "nova") which will take care of the EventDispatching___

1. ___Nova's EventHandlingBeanPostprocessor bean___

We do prefer annotation based ApplicationContext configurations, therefore we provide the convenience
class ```AnnotationEnablingConfiguration``` which makes providing the required
```EventHandlingBeanPostprocessor``` bean very easy. Simply import it in your custom configuration
class and you are ready to go. As an example:

```
@Configuration
@Import(AnnotationEnablingConfiguration)
public class MyConfig {
    @Bean
    public MyClass myBean() {
        return new MyClass();
    }

    @Bean
    public Nova nova() {
        ... // create Nova instance
    }

    ... // further bean definitions
}
```

The same approach can be taken to create the required ```Nova``` bean. Since our artifact depends on
[spring-support](../spring-support/README.md), you can also make use of the provided
```NovaProvidingConfiguration``` class. Simply import this configuration to make your custom ```Configuration```
class even simpler:

```
@Configuration
@Import({NovaProvidingConfig.class, AnnotationEnablingConfiguration})
public class MyConfig {
    @Bean
    public MyClass myBean() {
        return new MyClass();
    }

    ... // further bean definitions
}
```

__But wait... there's one more thing!__

Since we firmly believe that a modern software system should capture as much metric data as
possible, we wanted to make it easy for every event handler to capture its own specific metrics.
Our solution to this is that your are able to add an additional parameter to your handling method.
If you do so, and

- the parameter is of type ```EventContext```
- and it's the last parameter in your event handling method signature,

the system automatically injects an ```EventContext``` instance whenever the handler is invoked. This
context object provides convenient access to Nova's ```Metrics``` and ```EventEmitter```.

As an example, we could change the code from above to look like this:

```
public class MyClass {
    ...

    @OnEvent("myEvent")
    public void myEventHandlerMethod (String parameter1, double parameter 2, EventContext context) {
        ...
        context.metrics.getCounter("myCounter").inc();
        ...
    }
}
```

This would have the effect, that whenever somebody emits the ```"myEvent"``` event, the ```myEventHandler```
method is called with an appropriate context object which - in our example - is used to increment the
counter named ```myCounter```.
