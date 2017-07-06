core
---

This package provides the core functionality, all other Nova modules build upon.
 
The core functionality contains
* file system access
* metrics
* a (reactive) event bus

### Usage
To make use of the features provided, the first thing you have to do is create a new 
```Nova``` instance:
 
```
Nova nova = Nova.builder().build();
```

That's it. Via this instance, you then have access to the described functionality 
by accessing the appropriate fields:

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

* ```captureJvmMetrics``` - Specifies, whether Nova should automatically capture JVM metrics (memory usage, CPU usage 
and garbage collection stats). Default value is ```true```.


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

