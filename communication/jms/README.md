jms
=========

Reactive abstractions over JMS.

## 1. What is this about?
This package provides the ```JmsAdapter``` class which can be used to 
send and retrieve messages via JMS. Pub/sub, point-to-point as well as
RPC-style communication patterns are supported.

## 2. JmsAdapter instantiation

To instantiate a new ```JmsAdapter``` use the Builder (obtain it by invoking the ```builder()``` method) and pass the 
following parameters: 

  | Parameter       | Description                                                                                       |
  |-----------------------------------------|---------------------------------------------------------------------------------------------------|
  | defaultDeliveryMode | the default message delivery mode (can be overridden on a call-by-call basis)                     |
  | defaultPriority       | the default message priority (can be overridden on a call-by-call basis)                          |
  | defaultTimeToLive   | the default message TTL (can be overridden on a call-by-call basis)                               |
  | consumerSessionAckMode      | the ACK mode for received messages | Session.AUTO_ACKNOWLEDGE                                     |
  | producerSessionAckMode      | the ACK mode for sent messages | Session.AUTO_ACKNOWLEDGE                                         |
  | defaultRpcTimeout | the default timeout when firing RPC requests. Default is 30 seconds.|
  | identifier             | the identifier to assign to the JmsAdapter.                                                       | 
  | | | |
  | connectionFactory            | the factory to use to connect to the message broker.                                                      |  
  | correlationIdGenerator      | Every message will be sent via the JmsAdapter using a unique correlation ID, obtained from this instance. If not provided, a default UUID provider will be used |
  | destinationIdGenerator      | Destination objects are cached internally, and for that we use a (unique) ID obtained from this instance. If not provided a default ID provider is used. only override if you know what you're doing |

Of course you can also instantiate a new ```JmsAdapter``` instance programmatically. To do so,
make use of the ```Builder``` that can be obtained by invoking the builder method
 
## 3. Usage

...
