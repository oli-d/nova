jms
=========

Reactive abstractions over JMS.

## 1. What is this about?
This package provides the ```JmsAdapter``` class which can be used to 
send and retrieve messages via JMS. Pub/sub, point-to-point as well as
RPC-style communication patterns are supported.

## 2. JmsAdapter instantiation

The easiest way to retrieve a ```JmsAdapter``` instance is using Spring. For this, we provide
the class ```JmsEnablingConfiguration```. Simply import this in your own Spring configuration
and you can het a bean called "jmsAdapter" from the ApplicationContext.

When you do so, default configuration will be applied, which can be overridden via
environment variables or by providing the appropriate beans yourself. The possible
configuration values are

  | @Bean name                    | Environnment variable name              | Description                                                                   | Default value |
  |-------------------------------|-----------------------------------------|-------------------------------------------------------------------------------|---------------|
  | defaultMessageDeliveryMode    | NOVA.JMS.DEFAULT_MESSAGE_DELIVERY_MODE  | the default message delivery mode (can be overridden on a call-by-call basis) | Message.DEFAULT_DELIVERY_MODE |
  | defaultMessagePriority        | NOVA.JMS.DEFAULT_MESSAGE_PRIORITY       | the default message priority (can be overridden on a call-by-call basis)      | Message.DEFAULT_PRIORITY |
  | defaultMessageTimeToLive      | NOVA.JMS.DEFAULT_MESSAGE_TIME_TO_LIVE   | the default message TTL (can be overridden on a call-by-call basis)           | Message.DEFAULT_TIME_TO_LIVE |
  | consumerSessionAckMode        | NOVA.JMS.CONSUMER_SESSION_ACK_MODE      | the ACK mode for received messages | Session.AUTO_ACKNOWLEDGE                 |
  | producerSessionAckMode        | NOVA.JMS.PRODUCER_SESSION_ACK_MODE      | the ACK mode for sent messages | Session.AUTO_ACKNOWLEDGE                     |
  | defaultJmsRpcTimeoutInSeconds | NOVA.JMS.DEFAULT_RPC_TIMEOUT_IN_SECONDS | the default timeout in seconds when firing RPC requests                       | 30 |
  | jmsAdapterIdentifier          | NOVA.JMS.ADAPTER_IDENTIFIER             | the identifier to assign to the JmsAdapter.                                   | <null> |
  | jmsAdapterSettings            | n/a                                     | a ```JmsAdapterSettings``` instance, containing all aforementioned config values. Handy if you want to read the configuration or override multiple defaults programmatically. |  |
  | | | | |
  | jmsMessageTranscriber         | n/a                                     | the transcriber to use for incoming / outgoing messages                       | default transcriber, see below |
  | jmsCorrelationIdGenerator     | n/a                                     | Every message will be sent via the JmsAdapter using a unique correlation ID, obtained from this instance. | default UUID provider |
  | jmsDestinationIdGenerator     | n/a                                     | Destination objects are cached internally, and for that we use a (unique) ID obtained from this instance. | default ID provider, only override if you know what you're doing |

Of course you can also instantiate a new ```JmsAdapter``` instance programmatically. To do so,
make use of the ```Builder``` that can be obtained by invoking the builder method
 
```
Builder builder = JmsAdapter.builder();
```

Under the hood, an ```HttpServer``` is used to listen to incoming requests. Refer to the 
documentation of the [http](../http/README.md) module to see how this is configured.

## 3. Usage

...

### 3.3. Message "transcription"

Nessage transcription works exactly as described in detail in the [http](../http/README.md) module. 
