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

  | @Bean name                    | Environnment variable name              | Description                                                           | Default value |
  |-------------------------------|-----------------------------------------|-----------------------------------------------------------------------|---------------|
  | kafkaPollTimeoutInMilliseconds| NOVA.KAFKA.POLL_TIMEOUT_IN_MILLISECONDS | the timeout in milliseconds when polling new messages from the broker | 1000 (1s) |
  | kafkaServerAddress            | NOVA.KAFKA.SERVER_ADDRESS               | the address of the server to connect to                               | Message.DEFAULT_PRIORITY |
  | kafkaAdapterIdentifier        | NOVA.KAFKA.ADAPTER_IDENTIFIER           | the identifier to assign to the adapter                               | <null> |
  | kafkaAdapterSettings          | n/a                                     | a ```KafkaAdapterSettings``` instance, containing all aforementioned config values. Handy if you want to read the configuration or override multiple defaults programmatically. |  |
  | | | | |
  | kafkaMessageTranscriber       | n/a                                     | the transcriber to use for incoming / outgoing messages               | default transcriber, see below |

Of course you can also instantiate a new ```KafkaAdapter``` instance programmatically. To do so,
make use of the ```Builder``` that can be obtained by invoking the builder method
 
```
Builder builder = KafkaAdapter.builder();
```


## 3. Usage

...

### 3.3. Message "transcription"

Nessage transcription works exactly as described in detail in the [http](../http/README.md) module. 
