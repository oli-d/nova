kafka
=========

Reactive abstractions over Kafka.

## 1. What is this about?
This package provides the ```KafkaAdapter``` class which can be used to 
send and retrieve messages via Kafka. 

## 2. KafkaAdapter instantiation

The easiest way to retrieve a ```KafkaAdapter``` instance is using Spring. For this, we provide
the class ```KafkaEnablingConfiguration```. Simply import this in your own Spring configuration
and you can get a bean called "kafkaAdapter" from the ApplicationContext.

When you do so, default configuration will be applied, which can be overridden via
environment variables or by providing the appropriate beans yourself. The possible
configuration values are

  | Environnment variable / bean name       | Description                                                           | Default value |
  |-----------------------------------------|-----------------------------------------------------------------------|---------------|
  | NOVA.KAFKA.POLL_TIMEOUT_IN_MILLISECONDS | the timeout in milliseconds when polling new messages from the broker | 1000 (1s) |
  | NOVA.KAFKA.SERVER_ADDRESS               | the address of the server to connect to                               | Message.DEFAULT_PRIORITY |
  | NOVA.KAFKA.ADAPTER_IDENTIFIER           | the identifier to assign to the adapter                               | <null> |
  | NOVA.KAFKA.ADAPTER_SETTINGS             | a ```KafkaAdapterSettings``` instance, containing all aforementioned config values. Handy if you want to read the configuration or override multiple defaults programmatically. |  |
  | | | |
  | NOVA.KAFKA.OBJECT_MAPPER                | the ObjectMapper to use when transcribing incoming / outgoing messages| default ObjectMapper, for details see [here](../comm/README.md) |
  | NOVA.KAFKA.CONSUMER_PROPERTIES          | the properties to use to create the Kafka consumer                    |  |
  | NOVA.KAFKA.PRODUCER_PROPERTIES          | the properties to use to create the Kafka producer                    |  |
  | NOVA.KAFKA.MESSAGE_TRANSCRIBER          | the transcriber to use for incoming / outgoing messages               | default transcriber, for details see [here](../comm/README.md) |
  | | | |
  | NOVA.KAFKA.ADAPTER                      | the ```KafkaAdapter``` instance                                       |  |


Of course you can also instantiate a new ```KafkaAdapter``` instance programmatically. To do so,
make use of the ```Builder``` that can be obtained by invoking the builder method
 
```
Builder builder = KafkaAdapter.builder();
```


## 3. Usage

...

