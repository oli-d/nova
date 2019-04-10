comm 
=========

Async and request/response style communication for Nova. 

This package is not intended to be used on its own. Moreover, it is just providing the 
building blocks that enable protocol - specific communication (like jms or http). It 
ensures, that we have the same programming patterns for all supported communication
protocols, therefore making it easy for you to switch between them.

## Message Transcription

All protocols we support currently use Strings as the wire format.   

In our Java code, we want to use our specific domain objects, which then have to be
serialized and deserialized to and from the wire format. This is what we call message 
transcription and what is the purpose of the ```MessageTranscriber``` class.

Therefore, to be able to create a String representation for a domain object or create a 
domain object from its String representation, every communication "Adapter" will make 
use of such a ```MessageTranscriber```.

If you do not provide a specific one, the library will instantiate a default one for you. The
default transcriber is using the [Jackson JSON library](https://github.com/FasterXML/jackson)
to convert your domain objects.

As convenient as this is, the default ```ObjectMapper``` configuration might not be
suitable for you and you need to apply your custom configuration. This can be done
by getting the protocol specific default instance from the ApplicationContext or
simply providing your own bean.

If you do not want to use the Jackson library at all, you can simply provide your own
MessageTranscriber bean that will then be used instead.

If you are happy with the default transcriber, but only want to override the handling for
specific types, you can programmatically do so using the ```registerClassSpecificTranscribers()``` 
method:
 
```
MessageTranscriber.registerClassSpecificTranscribers(
    Class<T> targetClass,
    Function<T, TransportMessageType> outgoingMessageTranscriber,
    Function<TransportMessageType, T> incomingMessageTranscriber)
```

or alternatively provide your own transcriber on a call-by-call basis.


