metrics-kafka

---

This artifacts provides the ```KafkaMetricsReporter``` utility that makes
it easy to publish metrics to Kafka.
 
### Usage

The ```KafkaMetricsReporter``` is used exactly the same way as the
```ElasticMetricsReporter```. Please have a look at its
[documentation](../metrics-elastic/README.md) to learn about the concepts and
usage.

Next to the fact that both classes deal with two different target systems
(and thus protocols), the wire format is also slightly different.

```KafkaMetricsReporter``` uses a ```MetricsDumpToMapConverter``` from the
[metrics-serialization](./metrics-serialization/README.md) module to convert
Metrics dump into a Map representation. This is a bit more generic than
the Elasticsearch specific format (in which e.g. timestamps must be
expressed in UTC and passed in the ```@timestamp``` field), and can therefore
be used more widely.
