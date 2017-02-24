# Nova

### 1. Why?
The goal of this project is to provide a small and easy-to-use 
library that enables developers to rapidly build systems or 
services in an "event driven, asynchronous and reactive style". 

The origins of the API were heavily influenced by Node.js and 
its underlying programming model, hence the name (***No***de for
Ja***va***). This programming model, which (for those who have 
no experience with Node.js) is based on single-threaded event 
processing, prove itself to be very helpful in eliminating 
nasty concurrency bugs and to allow the programmer to fully 
concentrate on the business logic.

However, as everything in life, this also came with a downside.
Mainly, there were three things we had to fight with 
1. call back hell for business logic that depends on multiple
data sources,
1. the need to split a long running execution into various parts
and emit artificial "sub events", and 
1. improper event processing can easily block your one and only
 thread, rendering the whole service unresponsive
 
Therefore we changed the philosophy (and implementation) of the 
library and based it on RxJava (2.0) to promote a fully reactive
way of programming.
 
* Being reactive makes it very easy to express business logic based
on various information sources and define "if this than that" scenarios.
* To the client, it is completely transparent whether information is
delivered synchronously or asynchronously 
* The client is under full control over the threading model. It can
 chose to go single threaded (if it has to mutate shared state) or
 multi threaded (for full system performance) at will. Going 
 fully functional even eliminates that question  

This change also allowed the library to shrink significantly. It currently
mainly provides the following functionality
1. Filesystem access to read data reactively
1. Metrics to easily collect information about your service
1. An EventBus that you can use to listen to and emit events


### 2. Usage
To use the library, the first thing you have to do is create a new Nova instance:
 
```
Nova nova = Nova.builder().build();
```

That's it. From this instance, you then have access to the described functionality 
by accessing the appropriate instance variables:

```
Filesystem fs = nova.filesystem;
Metrics metrics = nova.metrics;
EventBus eventBus = nova.eventBus;
```

There's only a very limited set of things that you can / need to configure when creating
the Nova instance. You do so by calling the appropriate setters on the Builder, e.g.

```
Nova nova = Nova.builder().setIdentifier("myId").build();
```

This is the list of parameters you can configure:

* ```identifier``` - Sets an identifier, which will be used for logging and metrics. Very useful if you plan to use 
multiple Nova instances in your VM. Default value is ```"Nova"```

* ```warnOnUnhandledEvents``` - Specifies, whether a warning should be logged, if an event was emitted on the EventBus, 
but no subscribers existed. Default value is ```false```.

* ```defaultBackpressureStrategy``` - Defines the default behaviour (which can be overriden whenever you subscribe for 
events) of the system, when an Event producer outperforms a consumer. The value must be one of the constants defined in 
io.reactivex.BackpressureStrategy (default value is ```BUFFER```), which are:

| Parameter name | Description |
|----------------|-------------|
| BUFFER | All events are buffered until they are finally consumed by the observer. Note that this can easily cause OutOfMemoryExceptions. |
| DROP | All events that cannot be dispatched to the consumer will be discarded. |
| ERROR | Signals an error, whenever an event cannot be dispatched. |
| LATEST | Only keeps the latest value, overwriting all previous values that could not bee dispatched to the consumer yet. |


#### Filesystem

Filesystem is just a small utility class that you can use to quickly read text files from either the file 
system or classpath. Do so, by calling the appropriate method and subscribe to the returned Single:
 
```
filesystem.readFile("path/to/someFile.txt")
        .subscribe(contents -> processContents(contents),
                   error -> processError(error));

```

You can also write files using the writeFile() methods. There's two flavours, that allow for asynchronous 
(```writeFile()```) or synchronous (```writeFileSync()```) file writing. Both return a Completable that can be checked 
for completion or error.

```
filesystem.writeFile("content", "isntThere.txt")
        .subscribe(() -> logger.info("File successfully written"),
                   error -> processError(error));
```

#### Metrics

Small wrapper around the codahale / dropwizard metrics package. 

#### EventBus

An in-memory EventBus with an easy-to-use, reactive API, providing metrics that allow for convenient system health monitoring. 

To listen to / consume events, simply call the ```on()``` method and subscribe the returned ```Flowable``` object:

```
 eventBus.on("myEvent").subscribe(eventData -> logger.info("Hey, I saw an event"));
```

_Note that the ```on()``` method is overloaded. It allows you to pass in a specific BackpressureStrategy that will be used 
for this particular subscription._

Whenever you no longer need to be informed about new events, just invoke ```dispose()``` on the ```Disposable``` you 
retrieved on subscription

```
Disposable subscription = eventBus.on("myEvent").subscribe(eventData -> processMyEvent(eventData));
/* ... */
subscription.dispose();

```
As you saw, whenever the subscriber gets informed, an Object array with additional data is passed. This additional data 
is provided by the object that emitted the event. E.g., let's assume we have a Person repository that would like to emit 
an event whenever a new Person has been added, providing the new person's first and last names. In this scenario it 
would use the EventBus' emit() method like this:
 
```
eventBus.emit("NewPersonCreated", newPerson.firstName, newPerson.lastName);
```

_(It is a good idea to use constants and NOT a magic String like shown above.)_

An interested party could then subscribe to the EventBus and - assuming that first and last names are Strings - process 
the information like this:

```
eventBus.on("NewPersonCreated").subscribe(data -> newPersonCreated((String)data[0],(String)data[1]);
```

Since working with this object array is not particularly convenient, there are a few wrapper classes that help with 
casting and make the code much more readable:

```
TwoParameterConsumer<String, String> newPersonConsumer = (firstName, lastName) -> newPersonCreated(firstName, lastName);
eventBus.on("NewPersonCreated").subscribe(newPersonConsumer);
```

Another big advantage of using those wrapper classes is the fact, that they catch and log all uncaught exceptions that 
occur during event consumption. Throwing an exception during event consumptions immediately voids your subscription. So, 
by using the wrapper classes you are sure, that even if your consumer had a problem for one particular events, it will 
still continue to receive all following events.

### 3. What else?
This is just the core library used by the other "micro frameworks" in the Nova
world. Those other libraries are tools that try to solve one (and only one) problem in a consistent way, making it
easy to combine them. They provide solutions for common, technical tasks, allowing developers to focus on the 
business features provided by a particular service. Currently existing libraries are

* [nova-comm](https://github.com/oli-d/nova-comm): communication base library, protocol agnostic reactive message sending and retrieval
* [nova-jms](https://github.com/oli-d/nova-jms): reactive JMS messaging
* [nova-http](https://github.com/oli-d/nova-http): reactive HTTP communication
* [nova-event-annotations](https://github.com/oli-d/nova-event-annotations): allows your spring beans to automatically connect to the EventBus using annotations
* [nova-service](https://github.com/oli-d/nova-service): Small service skeleton with a defined lifecycle and easy access to Nova and its EventBus 
* ...
