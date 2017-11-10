# Nova [![Build Status](https://travis-ci.org/oli-d/nova.svg?branch=master)](https://travis-ci.org/oli-d/nova)

### 1. Why?
The goal of this project is to provide a small and easy-to-use 
library that enables developers to rapidly build systems or 
services in an "event driven, asynchronous and reactive style". 

The origins of the API were heavily influenced by Node.js and 
its underlying programming model, hence the name (**No**de for
Ja**va**). This programming model, which (for those who have 
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


### 2. What's included?

Nova provides a couple of very small libraries. Those libraries try to focus on one (and only one) problem and try to
solve it in a consistent way, making it easy to combine them. They provide solutions for common, technical problems, 
allowing service developers to concentrate on the required business logic. 

Currently, the following libraries exist:

- Core functionality
  * [core](./core/README.md): core functionality, used by all other components
  * [spring-support](./spring-support/README.md): convenient creation of Nova bean with Spring
  * [event-annotations](./event-annotations/README.md): allows your spring beans to automatically connect to the EventBus using annotations

- Communication
  * [comm](./comm/README.md): communication base library providing protocol agnostic, reactive message sending and retrieval
  * [jms](./jms/README.md): reactive JMS messaging
  * [http](./http/README.md): reactive HTTP communication
  * [http-spring](./http-spring/README.md): convenience classes to easily enable HTTP communication with spring
  * [rest-annotations](./rest-annotations/README.md): annotation based REST communication
  * [websockets](./websockets/README.md): reactive WebSocket communication
  * [websockets-annotations](./websockets-annotations/README.md): annotation based WebSocket communication
  * [kafka](./kafka/README.md): reactive Kafka communication

- Service related
  * [service](./service/README.md): Small service skeleton with a defined lifecycle and easy access to Nova and its EventBus 
  * [metrics-serialization](./metrics-serialization/README.md): Utility to easily serialize MetricDumps
  * [metrics-elastic](./metrics-elastic/README.md): Utility to push Metrics to Elasticsearch
  * [metrics-kafka](./metrics-kafka/README.md): Utility to push Metrics to Kafka

