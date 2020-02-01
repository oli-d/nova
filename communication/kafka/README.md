kafka
=========

Reactive abstractions over Kafka.

## 1. What is this about?
This package provides the ```KafkaAdapter``` class which can be used to 
send and retrieve messages via Kafka. 

## 2. KafkaAdapter instantiation

To instantiate a new ```KafkaAdapter``` instance programmatically,
make use of the ```Builder``` that can be obtained by invoking the ```builder()``` method
 
The possible configuration values are

  | Parameter          | Description                                                                |
  |--------------------|----------------------------------------------------------------------------|
  | pollTimeout        | the timeout when polling new messages from the broker. Default is 1 second |
  | serverAddress      | the address of the server to connect to                                    |
  | identifier         | the identifier to assign to the adapter                                    |
  | | | |
  | brokerClientId     | the ID to use when connecting to the Kafka broker                          |
  | consumerProperties | the properties to use to create the Kafka consumer                         |
  | consumerGroupId    | the consumer group ID to use when polling from the broker                  |
  | producerProperties | the properties to use to create the Kafka producer                         |
  | messageTranscriber | the transcriber to use for incoming / outgoing messages.                   |


## 3. Usage

...

