Modern software systems are very often designed in a distributed,
service oriented manner. Systems, designed for high performance
and throughput, very often share two fundamental architectural
features:

I.) Parallel event processing

For high throughput, simply do multiple things at the same time.
Often, parallelism is reached by running multiple instances of a
service, instead of heavily multi - threading a single service 
instance. One reason for this is the fact that proper concurrent 
programming is simply very hard to do. Staying in a single
threaded environment makes the code less error prone and a lot 
easier to test and debug.

II.) Async, event driven event processing

Being event driven, usually allows the code to be more easily
organized. The different parts of the system, dealing with the
various different business features, can  be very loosely
coupled around a common "event dispatcher" (a.k.a bus).

Processing the events asynchronously removes the need to poll
resources. This removes a lot of overhead from these resources
and lets a system scale much better.




The goal of this project is to provide a small, easy-to-use 
library, that enables developers to build systems or services
in the above described "single threaded, asynchronous, event 
driven style". 

The API is heavily influenced by Node.js and uses the same names
in many places. Due to the significant differences between Java 
and JavaScript, compromises will have to be made.

First and foremost, Java does not support first-class functions,
let alone closures. However, it should be easy enough to mimic
them with classes. Yes, the code is more verbose (i.e. harder to
read and understand) but this does not stop us from using the
desired programming model.

Developers, not familiar with JavaScript or Node.js can ignore
the last two paragraphs and should have a look at the provided
examples, located in src/test/java/com/dotc/examples