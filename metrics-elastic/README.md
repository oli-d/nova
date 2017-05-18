metrics-elastic

---

This artifacts provides the ```ElasticMetricsReporter``` utility that makes 
it easy to publish metrics to ElasticSearch.
 
### Usage

The ```ElasticMetricsReporter``` class is a RxJava Consumer implementation that 
can be used together with ```Nova```'s  ```Metrics``` class to easily report
  all metrics to an ElasticSearch cluster.
   
The following code snippet shows how to do that:
```
// create Nova instance
Nova nova = ...;

// create ElasticMetricsReporter
ElasticMetricsReporter elasticMetricsReporter = new ElasticMetricsReporter(...);

// wire things up 
nova.metrics.dumpContinuously(5, TimeUnit.SECONDS)
   .doOnSubscribe(disposable -> elasticMetricsReporter.startup())
   .doOnDispose(() -> elasticMetricsReporter.shutdown())
   .subscribe(elasticMetricsReporter);
```

The constructor of the ```ElasticMetricsReporter``` takes the following parameters:

* ```elasticServer``` - The elastic server you want to upload the metrics to
* ```elasticPort``` - The elastic server port (usually 9300)
* ```clusterName``` - Name of the cluster you want to use
* ```indexName``` - Name of the Elastic index, you want to store the metrics in
* ```additionalMetricAttributes``` - This is an optional parameter. If you provide it, 
all entries of the provided map will be added to every metric before it is sent to 
Elastic. This can be useful if you want to filter metrics in Elastic based on those
additional attributes.

Since the ```ElasticMetricsReporter``` does not automatically connect to / disconnect
from the specified ElasticSearch server, it is good practice to trigger that when the 
Metrics Observable is subscribed / disposed, as shown in the example code above. 

Before each metric is uploaded to ElasticSearch, the ```ElasticMetricsReporter``` enriches
  it with some useful data. In addition to the metric attributes, the following fields
  are sent:
* ```_type``` - The metric's class' simpleName()
* ```name``` - The metric name
* ```@timestamp``` - Timestamp (in UTC) the data was sampled
* ```host``` - Current machine name
* ```hostAddress``` - Current machine's IP address
* all additional metric attributes passed into the constructor 

So, assuming we have a ```Meter``` metric named "myMetric" and we passed an addional
attribute "serviceName" with value "MyDemoService" into the constructor, this is how a 
the resulting document would look like in ElasticSearch (leaving out data added by Elasticsearch like ID, _score, etc...):
```
{
    ...
    "_type": "Meter",
    "count": 0,
    "fiveMinuteRate": 0,
    "fifteenMinuteRate": 0,
    "oneMinuteRate": 0,
    "meanRate": 0,
    "name": "myMetric",
    "@timestamp": "2017-12-24T13:50:20.000",
    "host": "mymachine",
    "hostAddress": "192.168.0.2",
    "serviceName": "MyDemoService",
    ...
}
```