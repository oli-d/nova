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

  | Environnment variable / bean name       | Description                                                                                       | Default value |
  |-----------------------------------------|---------------------------------------------------------------------------------------------------|---------------|
  | NOVA.JMS.DEFAULT_MESSAGE_DELIVERY_MODE  | the default message delivery mode (can be overridden on a call-by-call basis)                     | Message.DEFAULT_DELIVERY_MODE |
  | NOVA.JMS.DEFAULT_MESSAGE_PRIORITY       | the default message priority (can be overridden on a call-by-call basis)                          | Message.DEFAULT_PRIORITY |
  | NOVA.JMS.DEFAULT_MESSAGE_TIME_TO_LIVE   | the default message TTL (can be overridden on a call-by-call basis)                               | Message.DEFAULT_TIME_TO_LIVE |
  | NOVA.JMS.CONSUMER_SESSION_ACK_MODE      | the ACK mode for received messages | Session.AUTO_ACKNOWLEDGE                                     |
  | NOVA.JMS.PRODUCER_SESSION_ACK_MODE      | the ACK mode for sent messages | Session.AUTO_ACKNOWLEDGE                                         |
  | NOVA.JMS.DEFAULT_RPC_TIMEOUT_IN_SECONDS | the default timeout in seconds when firing RPC requests                                           | 30 |
  | NOVA.JMS.ADAPTER_IDENTIFIER             | the identifier to assign to the JmsAdapter.                                                       | <null> |
  | NOVA.JMS.ADAPTER_SETTINGS               | a ```JmsAdapterSettings``` instance, containing all aforementioned config values. Handy if you want to read the configuration or override multiple defaults programmatically. |  |
  | NOVA.JMS.ADAPTER                        | the ```JmsAdapter``` instance, built with the provided configuration                                      |  |
  | NOVA.JMS.AUTO_START_ADAPTER             | if set to ```true```, the JmsAdapter will automatically be started on ApplicationContext refresh          | false |
  | | | |
  | NOVA.JMS.CONNECTION_FACTORY             | the factory to use to connect to the message broker.                                                      |  |
  | NOVA.JMS.OBJECT_MAPPER                  | the ObjectMapper to use when transcribing incoming / outgoing messages                                    | default ObjectMapper, for details see [here](../comm/README.md) |
  | NOVA.JMS.MESSAGE_TRANSCRIBER            | the transcriber to use for incoming / outgoing messages                                                   | default transcriber, for details see [here](../comm/README.md) |
  | NOVA.JMS.CORRELATION_ID_GENERATOR       | Every message will be sent via the JmsAdapter using a unique correlation ID, obtained from this instance. | default UUID provider |
  | NOVA.JMS.DESTINATION_ID_GENERATOR       | Destination objects are cached internally, and for that we use a (unique) ID obtained from this instance. | default ID provider, only override if you know what you're doing |

Of course you can also instantiate a new ```JmsAdapter``` instance programmatically. To do so,
make use of the ```Builder``` that can be obtained by invoking the builder method
 
```
Builder builder = JmsAdapter.builder();
```

Under the hood, an ```HttpServer``` is used to listen to incoming requests. Refer to the 
documentation of the [http](../http/README.md) module to see how this is configured.

## 3. Usage

...
