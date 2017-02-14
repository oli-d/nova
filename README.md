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

There's a few things that you can configure when creating the Nova instance:

** describe config

#### Filesystem

** describe Filesystem

#### Metrics

** describe Metrics

#### EventBus


### 3. What else?
This is just the core library used by the other "micro frameworks" in the Nova
world. Those other libraries are tools that really make it easy to quickly build
your services:

* nova-comm: communication base library, protocol agnostic reactive message sending and retrieval
* nova-jms - reactive JMS messaging
* nova-http - reactive HTTP communication
* nova-event-annotations - allows your spring beans to automatically connect to the EventBus using annotations
* nova-service -  
* ...

** describe and include links
